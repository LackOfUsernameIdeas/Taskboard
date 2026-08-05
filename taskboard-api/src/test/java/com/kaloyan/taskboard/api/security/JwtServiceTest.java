package com.kaloyan.taskboard.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "a-test-secret-that-is-long-enough-for-hmac-sha-256";

    @Test void generatesTokenThatIdentifiesAndValidatesItsSubject() {
        JwtService service = new JwtService(SECRET, 60_000);
        UserDetails alice = user("alice");
        String token = service.generateToken(alice);

        assertThat(service.extractUsername(token)).isEqualTo("alice");
        assertThat(service.isTokenValid(token, alice)).isTrue();
        assertThat(service.isTokenValid(token, user("bob"))).isFalse();
    }

    @Test void rejectsTokenSignedWithAnotherKey() {
        String token = new JwtService(SECRET, 60_000).generateToken(user("alice"));
        JwtService otherService = new JwtService("another-test-secret-that-is-long-enough-for-hmac-sha-256", 60_000);
        assertThatThrownBy(() -> otherService.extractUsername(token)).isInstanceOf(RuntimeException.class);
    }

    private static UserDetails user(String username) { return org.springframework.security.core.userdetails.User.withUsername(username).password("x").authorities("ROLE_USER").build(); }
}
