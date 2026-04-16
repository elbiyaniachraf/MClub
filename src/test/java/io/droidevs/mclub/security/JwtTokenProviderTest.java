package io.droidevs.mclub.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    @Test
    void generateAndParse_shouldReturnSameUsername() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "testSecretKeyWhichNeedsToBeLongEnoughForHS256Algorithm_1234567890");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 60_000);

        Authentication auth = new UsernamePasswordAuthenticationToken("user@example.com", "pw");
        String token = provider.generateToken(auth);

        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals("user@example.com", provider.getUsernameFromJWT(token));
    }

    @Test
    void validateToken_shouldReturnFalse_forInvalidToken() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "testSecretKeyWhichNeedsToBeLongEnoughForHS256Algorithm_1234567890");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 60_000);

        assertFalse(provider.validateToken("not-a-jwt"));
    }
}

