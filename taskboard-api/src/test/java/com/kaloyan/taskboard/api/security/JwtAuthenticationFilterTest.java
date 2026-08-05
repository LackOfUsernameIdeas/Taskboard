package com.kaloyan.taskboard.api.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.FilterChain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock JwtService jwtService;
    @Mock UserDetailsService userDetailsService;
    @Mock FilterChain chain;

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void passesThroughRequestWithoutBearerToken() throws Exception {
        new JwtAuthenticationFilter(jwtService, userDetailsService).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test void authenticatesValidBearerToken() throws Exception {
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername("alice").password("x").authorities("ROLE_USER").build();
        when(jwtService.extractUsername("token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);
        when(jwtService.isTokenValid("token", details)).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer token");

        new JwtAuthenticationFilter(jwtService, userDetailsService).doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        verify(chain).doFilter(any(), any());
    }

    @Test void doesNotAuthenticateInvalidToken() throws Exception {
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername("alice").password("x").authorities("ROLE_USER").build();
        when(jwtService.extractUsername("token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);
        when(jwtService.isTokenValid("token", details)).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer token");

        new JwtAuthenticationFilter(jwtService, userDetailsService).doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }
}
