package io.droidevs.mclub.service;
import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.*;
import io.droidevs.mclub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    public EventDto createEvent(EventDto dto, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Club club = clubRepository.findById(dto.getClubId()).orElseThrow(() -> new ResourceNotFoundException("Club not found"));
        Event event = Event.builder()
                .title(dto.getTitle()).description(dto.getDescription())
                .location(dto.getLocation()).startDate(dto.getStartDate())
                .endDate(dto.getEndDate()).club(club).createdBy(user).build();
        return eventMapper.toDto(eventRepository.save(event));
    }
    public List<EventDto> getEventsByClub(UUID clubId) {
        return eventRepository.findByClubId(clubId).stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    public Page<EventDto> getAllEvents(org.springframework.data.domain.Pageable pageable) {
        return eventRepository.findAll(pageable).map(eventMapper::toDto);
    }
}
