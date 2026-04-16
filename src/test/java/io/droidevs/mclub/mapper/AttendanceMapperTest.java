package io.droidevs.mclub.mapper;

import io.droidevs.mclub.domain.AttendanceMethod;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.EventAttendance;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.AttendanceRecordDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AttendanceMapperTest {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Test
    void toDto_mapsNestedEventAndUserFields() {
        Event event = Event.builder().id(UUID.randomUUID()).build();
        User user = User.builder().id(UUID.randomUUID()).email("s@example.com").build();

        EventAttendance a = EventAttendance.builder()
                .id(UUID.randomUUID())
                .event(event)
                .user(user)
                .checkedInAt(LocalDateTime.now())
                .method(AttendanceMethod.STUDENT_SCANNED_EVENT_QR)
                .build();

        AttendanceRecordDto dto = attendanceMapper.toDto(a);
        assertNotNull(dto);
        assertEquals(event.getId(), dto.getEventId());
        assertEquals(user.getId(), dto.getUserId());
        assertEquals("s@example.com", dto.getUserEmail());
        assertEquals(AttendanceMethod.STUDENT_SCANNED_EVENT_QR.name(), dto.getMethod());
    }
}

