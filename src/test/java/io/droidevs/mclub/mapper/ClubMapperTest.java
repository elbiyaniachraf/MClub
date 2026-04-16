package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.ClubDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ClubMapperTest {

    @Autowired
    private ClubMapper clubMapper;

    @Test
    void toDto_mapsNestedCreatedById() {
        User creator = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .build();

        Club club = Club.builder()
                .id(UUID.randomUUID())
                .name("Tech")
                .description("Desc")
                .createdBy(creator)
                .build();

        ClubDto dto = clubMapper.toDto(club);
        assertNotNull(dto);
        assertEquals(club.getId(), dto.getId());
        assertEquals("Tech", dto.getName());
        assertEquals(creator.getId(), dto.getCreatedById());
    }
}

