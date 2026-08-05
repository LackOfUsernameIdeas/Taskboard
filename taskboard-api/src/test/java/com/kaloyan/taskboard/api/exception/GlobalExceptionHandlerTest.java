package com.kaloyan.taskboard.api.exception;

import com.kaloyan.taskboard.core.exception.TaskNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void translatesDomainAndAuthenticationExceptionsToSafeHttpResponses() {
        var missing = handler.handleTaskNotFound(new TaskNotFoundException(12L));
        var duplicate = handler.handleUsernameAlreadyExists(new UsernameAlreadyExistsException("alice"));
        var invalidCredentials = handler.handleBadCredentials(new BadCredentialsException("details must not leak"));
        assertThat(missing.getStatusCode().value()).isEqualTo(404);
        assertThat(missing.getBody()).containsEntry("error", "Task not found with id: 12");
        assertThat(duplicate.getStatusCode().value()).isEqualTo(409);
        assertThat(duplicate.getBody()).containsEntry("error", "Username already taken: alice");
        assertThat(invalidCredentials.getStatusCode().value()).isEqualTo(401);
        assertThat(invalidCredentials.getBody()).containsEntry("error", "Invalid username or password");
    }
}
