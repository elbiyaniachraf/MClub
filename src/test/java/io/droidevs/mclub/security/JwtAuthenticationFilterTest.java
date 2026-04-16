package io.droidevs.mclub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "tokenProvider", tokenProvider);
        ReflectionTestUtils.setField(filter, "customUserDetailsService", userDetailsService);
    }

    @Test
    void shouldAuthenticate_whenValidJwtInAuthorizationHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer good");

        when(tokenProvider.validateToken("good")).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT("good")).thenReturn("u@example.com");
        when(userDetailsService.loadUserByUsername("u@example.com"))
                .thenReturn(new User("u@example.com", "pw", List.of(() -> "ROLE_STUDENT")));

        filter.doFilter(req, res, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("u@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(req, res);
    }

    @Test
    void shouldAuthenticate_whenValidJwtInCookie() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.setCookies(new Cookie("jwtToken", "cookieJwt"));

        when(tokenProvider.validateToken("cookieJwt")).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT("cookieJwt")).thenReturn("cookie@example.com");
        when(userDetailsService.loadUserByUsername("cookie@example.com"))
                .thenReturn(new User("cookie@example.com", "pw", List.of(() -> "ROLE_STUDENT")));

        filter.doFilter(req, res, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("cookie@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(req, res);
    }

    @Test
    void shouldNotAuthenticate_whenInvalidToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer bad");

        when(tokenProvider.validateToken("bad")).thenReturn(false);

        filter.doFilter(req, res, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(req, res);
    }

    @Test
    void shouldClearContext_whenUserDeleted_usernameNotFound() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer good");

        when(tokenProvider.validateToken("good")).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT("good")).thenReturn("ghost@example.com");
        when(userDetailsService.loadUserByUsername("ghost@example.com"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("gone"));

        filter.doFilter(req, res, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(req, res);
    }
}

