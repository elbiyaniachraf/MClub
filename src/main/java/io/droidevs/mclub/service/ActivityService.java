package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.Activity;
import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.ActivityDto;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.ActivityMapper;
import io.droidevs.mclub.repository.ActivityRepository;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ActivityMapper activityMapper;
    private final ClubAuthorizationService clubAuthorizationService;

    public ActivityDto create(ActivityDto dto, String email) {
        if (dto.getClubId() == null) {
            throw new IllegalArgumentException("clubId is required");
        }

        Club club = clubRepository.findById(dto.getClubId())
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        clubAuthorizationService.requirePlatformAdminOrClubAdminOrStaff(email, club.getId());

        User user = userRepository.findByEmail(email).orElseThrow();

        Event event = null;
        if (dto.getEventId() != null) {
            event = eventRepository.findById(dto.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            if (!event.getClub().getId().equals(club.getId())) {
                throw new IllegalArgumentException("eventId does not belong to this club");
            }
        }

        Activity a = Activity.builder()
                .club(club)
                .event(event)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .date(dto.getDate())
                .createdBy(user)
                .build();

        return activityMapper.toDto(activityRepository.save(a));
    }

    public List<ActivityDto> getByClub(UUID clubId) {
        return activityRepository.findByClubId(clubId).stream().map(activityMapper::toDto).collect(Collectors.toList());
    }

    public List<ActivityDto> getRecentByClub(UUID clubId) {
        return activityRepository.findTop5ByClubIdOrderByDateDesc(clubId).stream()
                .map(activityMapper::toDto)
                .collect(Collectors.toList());
    }
}
