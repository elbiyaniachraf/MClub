
package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final io.droidevs.mclub.mapper.EventCreateRequestMapper eventCreateRequestMapper;

    @PostMapping
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventCreateRequest request, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(eventService.createEvent(eventCreateRequestMapper.toDto(request), auth.getName()));
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<EventDto>> getEventsByClub(@PathVariable UUID clubId) {
        return ResponseEntity.ok(eventService.getEventsByClub(clubId));
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<RegistrationDto> registerEvent(@PathVariable UUID eventId, Authentication auth) {
        return ResponseEntity.ok(registrationService.register(eventId, auth.getName()));
    }

    @GetMapping("/{eventId}/registrations/summary")
    public ResponseEntity<EventRegistrationsSummaryDto> registrationsSummary(@PathVariable UUID eventId) {
        // tests + UI expect "count"
        return ResponseEntity.ok(new EventRegistrationsSummaryDto(eventId, registrationService.countRegistrations(eventId)));
    }

    @GetMapping("/{eventId}/participants")
    public ResponseEntity<List<RegistrationDto>> getParticipants(@PathVariable UUID eventId, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        // Only platform admin or club admin/staff can view full participant list
        eventService.requireCanManageEvent(auth.getName(), eventId);
        return ResponseEntity.ok(registrationService.getRegistrations(eventId));
    }
}
