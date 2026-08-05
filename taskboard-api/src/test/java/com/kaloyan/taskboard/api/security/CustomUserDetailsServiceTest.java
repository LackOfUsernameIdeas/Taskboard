package com.kaloyan.taskboard.api.security;

import com.kaloyan.taskboard.core.model.Role;
import com.kaloyan.taskboard.core.model.User;
import com.kaloyan.taskboard.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock UserRepository repository;
    @InjectMocks CustomUserDetailsService service;

    @Test void mapsPersistedUserToSpringSecurityDetails() {
        User user = new User(); user.setUsername("admin"); user.setPassword("hash"); user.setRole(Role.ADMIN);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));
        var details = service.loadUserByUsername("admin");
        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test void rejectsUnknownUser() {
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class).hasMessage("User not found: missing");
    }
}
