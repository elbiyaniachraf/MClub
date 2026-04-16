package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.*;
import io.droidevs.mclub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final EventEntityMapper eventEntityMapper;
    private final ClubAuthorizationService clubAuthorizationService;

    public EventDto createEvent(EventDto dto, String email) {
        if (dto.getClubId() == null) {
            throw new IllegalArgumentException("clubId is required");
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (dto.getLocation() == null || dto.getLocation().isBlank()) {
            throw new IllegalArgumentException("location is required");
        }
        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (dto.getEndDate() == null) {
            throw new IllegalArgumentException("endDate is required");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        Club club = clubRepository.findById(dto.getClubId())
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        clubAuthorizationService.requirePlatformAdminOrClubAdminOrStaff(email, club.getId());

        User user = userRepository.findByEmail(email).orElseThrow();

        // MapStruct maps simple scalar fields; service sets controlled relationships.
        Event event = eventEntityMapper.toEntity(dto);
        event.setClub(club);
        event.setCreatedBy(user);

        return eventMapper.toDto(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventDto> getEventsByClub(UUID clubId) {
        return eventRepository.findByClubId(clubId).stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventDto> getRecentEventsByClub(UUID clubId) {
        // Fetch joins (club + createdBy) to avoid LazyInitializationException during mapping
        return eventRepository.findRecentByClubIdWithClubAndCreatedBy(clubId).stream()
                .limit(5)
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<EventDto> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable).map(eventMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Event getEvent(UUID id) {
        return eventRepository.findByIdWithClub(id)
                .orElseGet(() -> eventRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found")));
    }

    public void requireCanManageEvent(String email, UUID eventId) {
        Event event = eventRepository.findByIdWithClub(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        clubAuthorizationService.requirePlatformAdminOrClubAdminOrStaff(email, event.getClub().getId());
    }
}
