package io.droidevs.mclub.controller;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final RegistrationService registrationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'CLUB_ADMIN')")
    public ResponseEntity<EventDto> createEvent(@RequestBody EventDto dto, Authentication auth) {
        return ResponseEntity.ok(eventService.createEvent(dto, auth.getName()));
    }
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<EventDto>> getEventsByClub(@PathVariable UUID clubId) {
        return ResponseEntity.ok(eventService.getEventsByClub(clubId));
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<RegistrationDto> registerEvent(@PathVariable UUID eventId, Authentication auth) {
        return ResponseEntity.ok(registrationService.register(eventId, auth.getName()));
    }
    @GetMapping("/{eventId}/participants")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'CLUB_ADMIN')")
    public ResponseEntity<List<RegistrationDto>> getParticipants(@PathVariable UUID eventId) {
        return ResponseEntity.ok(registrationService.getRegistrations(eventId));
    }
}
