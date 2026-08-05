package com.kaloyan.taskboard.api.controller;

import com.kaloyan.taskboard.api.dto.LoginRequest;
import com.kaloyan.taskboard.api.dto.RegisterRequest;
import com.kaloyan.taskboard.api.exception.UsernameAlreadyExistsException;
import com.kaloyan.taskboard.api.security.JwtService;
import com.kaloyan.taskboard.core.model.Role;
import com.kaloyan.taskboard.core.model.User;
import com.kaloyan.taskboard.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;
    @Mock JwtService jwtService;
    @InjectMocks AuthController controller;

    @Test void registersEncodedRegularUser() {
        RegisterRequest request = register("alice", "password1");
        when(passwordEncoder.encode("password1")).thenReturn("encoded");

        assertThat(controller.register(request).getStatusCode().value()).isEqualTo(201);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getPassword()).isEqualTo("encoded");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test void rejectsDuplicateUsernameBeforeEncodingPassword() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        assertThatThrownBy(() -> controller.register(register("alice", "password1")))
                .isInstanceOf(UsernameAlreadyExistsException.class).hasMessage("Username already taken: alice");
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test void authenticatesThenReturnsJwt() {
        LoginRequest request = login("alice", "password1");
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername("alice").password("x").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);
        when(jwtService.generateToken(details)).thenReturn("jwt");

        assertThat(controller.login(request).getBody().getToken()).isEqualTo("jwt");
        verify(authenticationManager).authenticate(argThat(token -> token.getName().equals("alice") && token.getCredentials().equals("password1")));
        verify(jwtService).generateToken(details);
    }

    private static RegisterRequest register(String username, String password) { RegisterRequest r = new RegisterRequest(); r.setUsername(username); r.setPassword(password); return r; }
    private static LoginRequest login(String username, String password) { LoginRequest r = new LoginRequest(); r.setUsername(username); r.setPassword(password); return r; }
}
