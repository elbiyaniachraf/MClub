package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.EventRating;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.EventRatingDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventRatingMapperTest {

    @Autowired
    private EventRatingMapper eventRatingMapper;

    @Test
    void toDto_mapsEventIdAndStudentEmail() {
        Event event = Event.builder().id(UUID.randomUUID()).build();
        User student = User.builder().id(UUID.randomUUID()).email("s@example.com").build();

        EventRating rating = EventRating.builder()
                .id(UUID.randomUUID())
                .event(event)
                .student(student)
                .rating(5)
                .comment("great")
                .build();

        EventRatingDto dto = eventRatingMapper.toDto(rating);
        assertNotNull(dto);
        assertEquals(event.getId(), dto.getEventId());
        assertEquals("s@example.com", dto.getStudentEmail());
        assertEquals(5, dto.getRating());
        assertEquals("great", dto.getComment());
    }
}

