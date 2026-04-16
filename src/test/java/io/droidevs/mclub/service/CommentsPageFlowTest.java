package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.CommentTargetType;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduces the web "View all comments" flow at the service layer.
 * If MapStruct mapping or comment tree shaping breaks, this should fail with the same exception
 * that would surface as HTTP 500.
 */
@SpringBootTest
class CommentsPageFlowTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void getThreadWithReplyPreview_doesNotThrow_forExistingEvent() {
        Event event = eventRepository.findAll().stream().findFirst().orElse(null);
        if (event == null) {
            // No seeded data in this test profile; just ensure method handles empty thread.
            assertDoesNotThrow(() -> commentService.getThreadWithReplyPreview(CommentTargetType.EVENT, UUID.randomUUID(), null, 1));
            return;
        }

        assertDoesNotThrow(() -> commentService.getThreadWithReplyPreview(CommentTargetType.EVENT, event.getId(), null, 1));
    }
}

