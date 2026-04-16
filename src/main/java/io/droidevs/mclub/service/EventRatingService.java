package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.EventRating;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.EventRatingDto;
import io.droidevs.mclub.dto.EventRatingRequest;
import io.droidevs.mclub.dto.EventRatingSummaryDto;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.EventRatingMapper;
import io.droidevs.mclub.repository.EventRatingRepository;
import io.droidevs.mclub.repository.EventRegistrationRepository;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventRatingService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventRatingRepository eventRatingRepository;
    private final UserRepository userRepository;
    private final AttendanceService attendanceService;
    private final EventRatingMapper eventRatingMapper;
    private final io.droidevs.mclub.mapper.EventRatingFactoryMapper eventRatingFactoryMapper;

    @Transactional
    public EventRatingDto rateEvent(UUID eventId, EventRatingRequest request, String email) {
        User student = userRepository.findByEmail(email).orElseThrow();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can rate events");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Rating is allowed at any time (before/during/after the event).
        // We still enforce eligibility rules (registered + attended).

        // eligibility: must be registered
        if (eventRegistrationRepository.findByUserIdAndEventId(student.getId(), eventId).isEmpty()) {
            throw new ForbiddenException("You must register for the event before rating it");
        }

        // eligibility: must have attended (checked-in)
        if (!attendanceService.hasAttended(eventId, student.getId())) {
            throw new ForbiddenException("Only students who attended can rate this event");
        }

        // Upsert: keep only the last rating per (event, student)
        EventRating rating = eventRatingRepository.findByEventIdAndStudentId(eventId, student.getId())
                .orElseGet(() -> {
                    EventRating r = eventRatingFactoryMapper.create();
                    r.setEvent(event);
                    r.setStudent(student);
                    return r;
                });

        rating.setRating(request.getRating());
        rating.setComment(request.getComment());

        return eventRatingMapper.toDto(eventRatingRepository.save(rating));
    }

    public EventRatingDto getMyRating(UUID eventId, String email) {
        User student = userRepository.findByEmail(email).orElseThrow();
        EventRating rating = eventRatingRepository.findByEventIdAndStudentId(eventId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found"));
        return eventRatingMapper.toDto(rating);
    }

    public EventRatingSummaryDto getSummary(UUID eventId) {
        Double avg = eventRatingRepository.getAverageRating(eventId);
        long count = eventRatingRepository.getRatingCount(eventId);
        return new EventRatingSummaryDto(eventId, avg == null ? 0.0 : avg, count);
    }

    public List<EventRatingDto> getAllRatingsForEvent(UUID eventId) {
        return eventRatingRepository.findByEventId(eventId).stream().map(eventRatingMapper::toDto).collect(Collectors.toList());
    }
}
