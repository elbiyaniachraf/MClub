package io.droidevs.mclub.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test to ensure newly introduced entity mappers are registered as Spring beans.
 */
@SpringBootTest
class EntityMappersCompileTest {

    @Autowired(required = false) private EventEntityMapper eventEntityMapper;
    @Autowired(required = false) private ActivityEntityMapper activityEntityMapper;
    @Autowired(required = false) private ClubEntityMapper clubEntityMapper;
    @Autowired(required = false) private MembershipEntityMapper membershipEntityMapper;
    @Autowired(required = false) private ClubApplicationEntityMapper clubApplicationEntityMapper;
    @Autowired(required = false) private EventRegistrationEntityMapper eventRegistrationEntityMapper;
    @Autowired(required = false) private UserEntityMapper userEntityMapper;

    @Test
    void mappersAreBeans() {
        assertNotNull(eventEntityMapper);
        assertNotNull(activityEntityMapper);
        assertNotNull(clubEntityMapper);
        assertNotNull(membershipEntityMapper);
        assertNotNull(clubApplicationEntityMapper);
        assertNotNull(eventRegistrationEntityMapper);
        assertNotNull(userEntityMapper);
    }
}

