# Taskboard

> **Multi-module task-management API**, split into a shared domain library, a REST layer, and a standalone **background worker** - combining stateless **JWT authentication** with **role-based access control** (`USER` or `ADMIN`). The worker runs independently on a 60-second schedule, polling the same **PostgreSQL** database to detect and log **overdue tasks**.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [API Reference](#api-reference)
- [Security](#security)
- [Gitignored Configuration Files](#gitignored-configuration-files)
- [Local Development](#local-development)

---

## Architecture

```
taskboard-parent/
├── taskboard-core/                            # Domain module shared by api and worker
│   ├── pom.xml
│   └── src/main/java/com/kaloyan/taskboard/core/
│       ├── model/                             # Task, User, Role, TaskStatus
│       ├── repository/                        # TaskRepository, UserRepository
│       ├── service/                           # TaskService (ownership-aware CRUD)
│       └── exception/                         # TaskNotFoundException
├── taskboard-api/                             # REST API - auth, controllers, JWT security (:8080)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/kaloyan/taskboard/api/
│       ├── controller/                        # AuthController, TaskController
│       ├── dto/                               # Request/response DTOs + mapper
│       ├── security/                          # SecurityConfig, JwtService, CustomUserDetailsService, JwtAuthenticationFilter
│       ├── config/                            # AdminSeeder - creates the initial admin account on startup (if there isn't one already)
│       └── exception/                         # GlobalExceptionHandler, UsernameAlreadyExistsException
├── taskboard-worker/                          # Background job - scans for overdue tasks
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/kaloyan/taskboard/worker/
│       ├── OverdueTaskChecker.java            # @Scheduled scan, every 60s
│       └── TaskboardWorkerApplication.java    # entry point, enables scheduling + JPA on shared core entities
├── docker-compose.yml
├── .env
└── pom.xml                                    # declares the three modules and shared Spring Boot version
```

| Sub-project        | Role                                                                                               |
| ------------------ |----------------------------------------------------------------------------------------------------|
| `taskboard-core`    | Shared domain library - entities, repositories, and services, used by both `api` and `worker`      |
| `taskboard-api`     | Public REST API for registration, login, and task CRUD operations, secured with stateless JWT auth |
| `taskboard-worker`  | Standalone scheduled process that polls the same database for overdue tasks and logs them          |

---

## Tech Stack

**API (`taskboard-api`)**  
Spring Boot (Web MVC, Spring Security, Spring Data JPA), PostgreSQL, JWT, BCrypt, Bean Validation

**Worker (`taskboard-worker`)**  
Spring Boot (Spring Data JPA, Spring Scheduling)

**Services & Tooling**  
Maven multi-module build, Docker, GitHub Actions, H2 (only for testing)

---

## API Reference

Base URL (local): `http://localhost:8080`

All endpoints except `/auth/register` and `/auth/login` require an `Authorization: Bearer <token>` header. 

This is the complete set of endpoints exposed by the API:

| Method | Path | Auth | Description                                                                    |
|---|---|---|--------------------------------------------------------------------------------|
| POST | `/auth/register` | none | Create a new `USER` account                                                    |
| POST | `/auth/login` | none | Exchange credentials for a JWT                                                 |
| GET | `/tasks` | any authenticated user | List your own tasks, or **all** tasks if you're an admin                       |
| GET | `/tasks/{id}` | any authenticated user | Fetch one task you own (or any task, if admin)                                 |
| POST | `/tasks` | any authenticated user | Create a task, owned by the caller (admins cannot assign tasks to other users) |
| PUT | `/tasks/{id}` | any authenticated user | Update a task you own (or any task, if admin)                                  |
| DELETE | `/tasks/{id}` | `ADMIN` only | Delete any task, regardless of owner                                           |

### `POST /auth/register`

**Request body**

| Field | Type | Rules |
|---|---|---|
| `username` | string | required, 3–50 characters |
| `password` | string | required, 8+ characters |

**Responses**

| Status | When |
|---|---|
| `201` | Account created. Empty body. Always creates a `USER` - this endpoint can never create an `ADMIN` |
| `400` | Validation failed. Body: `{"<field>": "<message>"}` per invalid field |
| `409` | Username already taken. Body: `{"error": "Username already taken: <username>"}` |

### `POST /auth/login`

**Request body**

| Field | Type | Rules |
|---|---|---|
| `username` | string | required |
| `password` | string | required |

**Responses**

| Status | When |
|---|---|
| `200` | Body: `{"token": "<jwt>"}` |
| `400` | Missing username or password |
| `401` | Bad credentials. Body: `{"error": "Invalid username or password"}` |

### `GET /tasks`

**Responses**

| Status | When                                              |
|---|---------------------------------------------------|
| `200` | Always. Body: array of task objects (see below) |

### `GET /tasks/{id}`

**Responses**

| Status | When |
|---|---|
| `200` | Task object (see below) |
| `404` | Task doesn't exist, **or** it exists but belongs to someone else and you're not an admin - the API deliberately returns the same `404` in both cases, rather than a `403`, so you can't tell whether a task ID you don't own actually exists |

### `POST /tasks`

**Request body**

| Field | Type | Rules |
|---|---|---|
| `title` | string | required, ≤100 characters |
| `description` | string | optional, ≤1000 characters |
| `status` | string | required, one of `TODO`, `IN_PROGRESS`, `DONE` |
| `dueDate` | string (`YYYY-MM-DD`) | optional |

**Responses**

| Status | When                                              |
|---|---------------------------------------------------|
| `201` | Created. Body: task object                        |
| `400` | Validation failed. Body: `{"<field>": "<message>"}` |

### `PUT /tasks/{id}`

Same request body and validation as `POST /tasks`. Full replace, not a partial update - any field you omit is overwritten with `null`/empty on the existing task.

**Responses**

| Status | When |
|---|---|
| `200` | Updated. Body: task object |
| `400` | Validation failed |
| `404` | Same not-found-or-not-yours behavior as `GET /tasks/{id}` |

### `DELETE /tasks/{id}`

Admin-only, enforced before the request reaches the controller.

**Responses**

| Status | When |
|---|---|
| `204` | Deleted. Empty body |
| `403` | Caller is authenticated but not an admin. **Empty body** - this one doesn't return the `{"error": "..."}` JSON shape used elsewhere, since it's rejected by Spring Security itself, before `GlobalExceptionHandler` ever runs |
| `404` | Task doesn't exist |

### Task object shape

```json
{
  "id": 1,
  "title": "Write tests",
  "description": null,
  "status": "TODO",
  "dueDate": "2026-09-01",
  "owner": "alice"
}
```

### Example requests

```bash
# Register
curl -X POST localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'

# Log in
curl -X POST localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
# -> {"token": "..."}

# Create a task
curl -X POST localhost:8080/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Write tests","status":"TODO","dueDate":"2026-09-01"}'

# List your tasks
curl localhost:8080/tasks -H "Authorization: Bearer $TOKEN"

# Update a task
curl -X PUT localhost:8080/tasks/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Write more tests","status":"IN_PROGRESS"}'

# Delete a task 
curl -i -X DELETE localhost:8080/tasks/1 -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## Security

- **Role-based access control** (`USER` or `ADMIN`) - regular users can only view and edit their own tasks; admins can view, edit, and delete any task. Delete operations are available only to admins, while non-admins are rejected before the request reaches the controller.
- **Password storage** - passwords are hashed with `BCryptPasswordEncoder`, never stored or logged in plaintext.
- **Stateless JWT auth** - every request except `/auth/**` requires a valid `Bearer` token, verified by a custom filter before Spring's own authentication runs. Tokens are signed with an HMAC key (`JWT_SECRET`) and expire after a configurable duration (default 1 hour).

---

## Gitignored Configuration Files

### `.env`

This file lives at the **repo root** and is excluded from version control and must exist locally before running the project. A committed `.env.example` shows the expected structure with placeholder values (see [Local Development](#local-development)).

It is consumed by `docker-compose.yml`, which injects each variable into the `app` and `worker` containers as an environment variable

```env
DB_NAME=taskdb
DB_USER=your_username
DB_PASSWORD=your_password

JWT_SECRET=replace_with_a_long_random_string_at_least_32_bytes

ADMIN_USERNAME=admin
ADMIN_PASSWORD=replace_with_a_strong_password
```

| Variable | Consumed by | Purpose                                                                                                                                                                                                         |
|---|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DB_NAME` | `postgres`, `app`, `worker` | Database name, shared by all three containers so `app` and `worker` connect to the same schema `postgres` creates                                                                                               |
| `DB_USER` / `DB_PASSWORD` | `postgres`, `app`, `worker` | Postgres credentials                                                                                                                                                                                            |
| `JWT_SECRET` | `app` only | HMAC signing key for every issued JWT (maps to `app.jwt.secret` in `taskboard-api`'s `application.properties`). Must be a long, random value (32+ bytes) - a short or predictable secret weakens token security |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `app` only | Read once by `AdminSeeder` on startup to create an initial `ADMIN` account. `/auth/register` can never produce an admin account by itself - this is the only path to one                                        |

---

## Local Development

### 1. Configure environment variables

**Linux / macOS**

```bash
cp .env.example .env
```

**Windows**

```cmd
copy .env.example .env
```

Fill in `.env` as described in [Gitignored Configuration Files](#gitignored-configuration-files).

### 2. Start everything with Docker Compose

```bash
docker compose up --build
```

This builds and starts three containers: `postgres`, `app` (`taskboard-api`, exposed on `http://localhost:8080`), and `worker` (`taskboard-worker`, no exposed port - it only talks to Postgres). 

Re-run with `--build` any time you change source code. Without it, Compose reuses the last-built image.

Watch the `app` container's logs for `Seeded initial admin account '<username>'` to confirm the admin account from `.env` was created. On every later startup where an admin already exists, `AdminSeeder` does nothing.

### 3. Verify it's running

```bash
# Log in as the admin account from .env
curl -X POST localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}"
# -> {"token": "..."}

# Use that token to list tasks (empty array on a fresh database)
curl localhost:8080/tasks -H "Authorization: Bearer <token from above>"
```

### 4. Run tests (optional)

```bash
./mvnw test
```

Tests run against an in-memory H2 database (`taskboard-api/src/test/resources/application.properties`), so no local Postgres is needed for this step.