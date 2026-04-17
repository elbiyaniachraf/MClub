package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.EventRatingDto;
import io.droidevs.mclub.dto.EventRatingRequest;
import io.droidevs.mclub.dto.EventRatingSummaryDto;
import io.droidevs.mclub.service.EventRatingService;
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

@WebMvcTest(EventRatingController.class)
@AutoConfigureMockMvc // enable Spring Security filters
@Import(io.droidevs.mclub.security.SecurityConfig.class)
class EventRatingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    EventRatingService eventRatingService;
    @MockBean EventService eventService;

    @Test
    void summary_shouldPermitAnonymous() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(eventRatingService.getSummary(eventId)).thenReturn(new EventRatingSummaryDto(eventId, 4.5, 2));

        mvc.perform(get("/api/events/{eventId}/ratings/summary", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void myRating_shouldReturn401_whenAnonymous() throws Exception {
        mvc.perform(get("/api/events/{eventId}/ratings/me", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "s@example.com", roles = "STUDENT")
    void rate_shouldAllowStudent() throws Exception {
        UUID eventId = UUID.randomUUID();
        EventRatingDto dto = new EventRatingDto();
        dto.setRating(5);
        when(eventRatingService.rateEvent(eq(eventId), any(EventRatingRequest.class), eq("s@example.com"))).thenReturn(dto);

        mvc.perform(post("/api/events/{eventId}/ratings", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "PLATFORM_ADMIN")
    void rate_shouldForbidNonStudent() throws Exception {
        mvc.perform(post("/api/events/{eventId}/ratings", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"ok\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "STUDENT")
    void list_shouldRequireCanManageEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(eventRatingService.getAllRatingsForEvent(eventId)).thenReturn(List.of());

        mvc.perform(get("/api/events/{eventId}/ratings", eventId))
                .andExpect(status().isOk());

        verify(eventService).requireCanManageEvent("manager@example.com", eventId);
    }
}
