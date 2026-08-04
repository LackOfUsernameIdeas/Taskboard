# Taskboard

> **Multi-module task-management API**, split into a shared domain library, a REST layer, and a standalone **background worker** - combining stateless **JWT authentication** with **role-based access control** (`USER` or `ADMIN`). The worker runs independently on a 60-second schedule, polling the same **PostgreSQL** database to detect and log **overdue tasks**.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Gitignored Configuration Files](#gitignored-configuration-files)
- [Local Development](#local-development)
- [Security](#security)
- [API Reference](#api-reference)

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

## Gitignored Configuration Files

This file is excluded from version control and must exist locally before running the project. The template below shows the expected structure - fill in real values yourself.

### `.env`

This file lives at the **repo root** and is consumed by `docker-compose.yml`, which injects the values into both the `app` (API) and `worker` containers as environment variables.

```env
DB_NAME=taskdb
DB_USER=your_username
DB_PASSWORD=your_password

JWT_SECRET=replace_with_a_long_random_string_at_least_32_bytes

ADMIN_USERNAME=admin
ADMIN_PASSWORD=replace_with_a_strong_password
```

> **Note on `JWT_SECRET`:** this signs and verifies every JWT the API issues. Use a long, random value (32+ bytes).

> **Note on `ADMIN_USERNAME` / `ADMIN_PASSWORD`:** these are only used once, by `AdminSeeder`, to create an initial admin account on first startup. Registration can never create an admin account by itself.

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

This starts three containers: Postgres, the API (`taskboard-api`, on `http://localhost:8080`), and the worker (`taskboard-worker`). 

Watch the logs for `Seeded initial admin account '<username>'` to confirm the admin account was created.

### 3. Run without Docker (optional)

You'll need a local Postgres instance matching `taskboard-api/src/main/resources/application.properties` (or overridden via env vars). Build all modules with the Maven wrapper from the repo root:

```bash
./mvnw clean package
```

Then run the api and worker jars separately:

```bash
java -jar taskboard-api/target/taskboard-api-0.0.1-SNAPSHOT.jar
java -jar taskboard-worker/target/taskboard-worker-0.0.1-SNAPSHOT.jar
```

### 4. Run tests (optional)

```bash
./mvnw test
```

Tests run against an in-memory H2 database (`taskboard-api/src/test/resources/application.properties`), so no local Postgres is needed for this step.

---

## Security

- **Role-based access control** (`USER` or `ADMIN`) - regular users can only view and edit their own tasks; admins can view, edit, and delete any task. Delete operations are available only to admins, while non-admins are rejected before the request reaches the controller.
- **Password storage** - passwords are hashed with `BCryptPasswordEncoder`, never stored or logged in plaintext.
- **Stateless JWT auth** - every request except `/auth/**` requires a valid `Bearer` token, verified by a custom filter before Spring's own authentication runs. Tokens are signed with an HMAC key (`JWT_SECRET`) and expire after a configurable duration (default 1 hour).

---

## API Reference

All endpoints except `/auth/**` require an `Authorization: Bearer <token>` header.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | none | Create a new `USER` account. `201` on success, `409` if the username is taken, `400` on validation errors. |
| POST | `/auth/login` | none | Exchange credentials for a JWT. `200` with `{"token": "..."}`, `401` on bad credentials. |
| GET | `/tasks` | any authenticated user | List your own tasks, or all tasks if you're an admin. |
| GET | `/tasks/{id}` | any authenticated user | Fetch one task. `404` if it doesn't exist. |
| POST | `/tasks` | any authenticated user | Create a task. |
| PUT | `/tasks/{id}` | any authenticated user | Update a task you own (or any task, if admin). |
| DELETE | `/tasks/{id}` | `ADMIN` only | Delete any task. `403` for non-admins, `204` on success. |

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

# Delete a task (requires an admin token)
curl -i -X DELETE localhost:8080/tasks/1 -H "Authorization: Bearer $ADMIN_TOKEN"
```