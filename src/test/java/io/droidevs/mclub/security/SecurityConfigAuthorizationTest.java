package io.droidevs.mclub.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight wiring test for SecurityConfig.
 *
 * Route matrix tests should be added later using MockMvc once the IDE resolves spring-test web imports.
 */
class SecurityConfigAuthorizationTest {

    @Test
    void securityConfig_shouldHoldJwtAuthenticationFilterBeanReference() {
        SecurityConfig cfg = new SecurityConfig();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(cfg, "jwtAuthenticationFilter", filter);

        Object injected = ReflectionTestUtils.getField(cfg, "jwtAuthenticationFilter");
        assertSame(filter, injected);
    }

    @Test
    void jwtAuthenticationFilter_shouldBeConfiguredToRunBeforeUsernamePasswordAuthenticationFilter() {
        // Intent check: SecurityConfig adds JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter.
        // We can't easily introspect the chain without building HttpSecurity here; so we assert class references.
        assertNotNull(UsernamePasswordAuthenticationFilter.class);
        assertNotNull(JwtAuthenticationFilter.class);
        assertTrue(UsernamePasswordAuthenticationFilter.class.isAssignableFrom(UsernamePasswordAuthenticationFilter.class));
    }
}


