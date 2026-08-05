package com.kaloyan.taskboard.api.config;

import com.kaloyan.taskboard.core.model.Role;
import com.kaloyan.taskboard.core.model.User;
import com.kaloyan.taskboard.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {
    @Mock UserRepository repository;
    @Mock PasswordEncoder encoder;

    @Test void doesNothingWhenAdminAlreadyExists() {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(true);
        new AdminSeeder(repository, encoder, "admin", "secret").run();
        verify(repository).existsByRole(Role.ADMIN);
        verifyNoMoreInteractions(repository); verifyNoInteractions(encoder);
    }

    @Test void createsEncodedAdminWhenNoneExists() {
        when(encoder.encode("secret")).thenReturn("hash");
        new AdminSeeder(repository, encoder, "admin", "secret").run();
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("admin");
        assertThat(saved.getValue().getPassword()).isEqualTo("hash");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test void promotesExistingUserWithConfiguredUsername() {
        User existing = new User(); existing.setId(7L); existing.setUsername("admin");
        when(repository.findByUsername("admin")).thenReturn(Optional.of(existing));
        when(encoder.encode("secret")).thenReturn("hash");
        new AdminSeeder(repository, encoder, "admin", "secret").run();
        verify(repository).save(existing);
        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
    }
}
