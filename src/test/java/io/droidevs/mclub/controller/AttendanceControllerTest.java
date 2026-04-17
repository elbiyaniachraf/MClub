package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.service.AttendanceService;
import io.droidevs.mclub.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
@AutoConfigureMockMvc // enable Spring Security filters
@Import(io.droidevs.mclub.security.SecurityConfig.class)
class AttendanceControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AttendanceService attendanceService;
    @MockBean
    EventService eventService;

    @Test
    void studentCheckIn_shouldReturn401_whenAnonymous() throws Exception {
        mvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"t\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "s@example.com", roles = "STUDENT")
    void studentCheckIn_shouldReturn200_whenStudent() throws Exception {
        AttendanceRecordDto dto = new AttendanceRecordDto();
        dto.setMethod("STUDENT_SCANNED_EVENT_QR");

        when(attendanceService.studentCheckInByEventQr(eq("raw"), eq("s@example.com"))).thenReturn(dto);

        mvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("STUDENT_SCANNED_EVENT_QR"));
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PLATFORM_ADMIN")
    void studentCheckIn_shouldReturn403_whenNotStudent() throws Exception {
        mvc.perform(post("/api/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "staff@example.com", roles = "STUDENT")
    void listAttendance_shouldCallRequireCanManageEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(attendanceService.listAttendance(eventId, "staff@example.com")).thenReturn(List.of());

        mvc.perform(get("/api/events/{eventId}/attendance", eventId))
                .andExpect(status().isOk());

        verify(eventService).requireCanManageEvent("staff@example.com", eventId);
    }
}
