package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.EventDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventMapperTest {

    @Autowired
    private EventMapper eventMapper;

    @Test
    void toDto_mapsClubIdAndCreatedById() {
        Club club = Club.builder().id(UUID.randomUUID()).name("Club").build();
        User creator = User.builder().id(UUID.randomUUID()).email("creator@example.com").build();

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .club(club)
                .createdBy(creator)
                .title("E")
                .description("D")
                .location("L")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();

        EventDto dto = eventMapper.toDto(event);
        assertNotNull(dto);
        assertEquals(event.getId(), dto.getId());
        assertEquals(club.getId(), dto.getClubId());
        assertEquals(creator.getId(), dto.getCreatedById());
        assertEquals("E", dto.getTitle());
    }

    @Test
    void toDto_nullSafe() {
        assertNull(eventMapper.toDto(null));
    }
}

