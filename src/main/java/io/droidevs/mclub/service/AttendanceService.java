package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.AttendanceMapper;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final EventAttendanceWindowRepository windowRepository;
    private final ClubAuthorizationService clubAuthorizationService;
    private final AttendanceTokenService attendanceTokenService;
    private final AttendanceMapper attendanceMapper;
    private final io.droidevs.mclub.mapper.EventAttendanceFactoryMapper eventAttendanceFactoryMapper;
    private final io.droidevs.mclub.mapper.EventAttendanceWindowFactoryMapper eventAttendanceWindowFactoryMapper;

    @Transactional
    public AttendanceEventQrDto openOrUpdateWindow(UUID eventId, AttendanceWindowRequest request, String organizerEmail) {
        Event event = getEvent(eventId);
        clubAuthorizationService.requirePlatformAdminOrClubAdminOrStaff(organizerEmail, event.getClub().getId());

        String rawToken = attendanceTokenService.generateRawToken();
        String tokenHash = attendanceTokenService.sha256Hex(rawToken);

        EventAttendanceWindow window = windowRepository.findByEventId(eventId)
                .orElseGet(() -> eventAttendanceWindowFactoryMapper.create(event));

        window.setActive(true);
        window.setOpensBeforeStartMinutes(request.getOpensBeforeStartMinutes());
        window.setClosesAfterStartMinutes(request.getClosesAfterStartMinutes());
        window.setTokenHash(tokenHash);
        window.setTokenRotatedAt(LocalDateTime.now());

        windowRepository.save(window);

        // We return the RAW token to be embedded into a QR code. Only the hash is stored.
        return new AttendanceEventQrDto(eventId, rawToken);
    }

    @Transactional
    public void closeWindow(UUID eventId, String organizerEmail) {
        Event event = getEvent(eventId);
        clubAuthorizationService.requirePlatformAdminOrClubAdminOrStaff(organizerEmail, event.getClub().getId());

        EventAttendanceWindow window = windowRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance window not found"));
        window.setActive(false);
        windowRepository.save(window);
    }

    @Transactional
    public AttendanceRecordDto studentCheckInByEventQr(String rawToken, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can check in");
        }

        String tokenHash = attendanceTokenService.sha256Hex(rawToken);
        EventAttendanceWindow window = windowRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired attendance QR"));

        if (!window.isActive()) {
            throw new ForbiddenException("Attendance window is closed");
        }

        Event event = window.getEvent();

        // Must be registered to the event
        if (eventRegistrationRepository.findByUserIdAndEventId(student.getId(), event.getId()).isEmpty()) {
            throw new ForbiddenException("You must be registered for the event to check in");
        }

        validateTimeWindow(event, window);

        // idempotent: if already checked-in, return existing record
        EventAttendance existing = eventAttendanceRepository.findByEventIdAndUserId(event.getId(), student.getId()).orElse(null);
        if (existing != null) {
            return attendanceMapper.toDto(existing);
        }

        EventAttendance attendance = eventAttendanceFactoryMapper.create(AttendanceMethod.STUDENT_SCANNED_EVENT_QR);
        attendance.setEvent(event);
        attendance.setUser(student);
        attendance.setCheckedInBy(null);

        return attendanceMapper.toDto(eventAttendanceRepository.save(attendance));
    }

    @Transactional
    public AttendanceRecordDto organizerCheckInStudent(UUID eventId, UUID studentId, String organizerEmail) {
        Event event = getEvent(eventId);
        clubAuthorizationService.requirePlatformAdminOrClubAdminOrStaff(organizerEmail, event.getClub().getId());

        EventAttendanceWindow window = windowRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance window not found"));
        if (!window.isActive()) {
            throw new ForbiddenException("Attendance window is closed");
        }

        validateTimeWindow(event, window);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can be checked in");
        }

        // Must be registered to the event
        if (eventRegistrationRepository.findByUserIdAndEventId(student.getId(), eventId).isEmpty()) {
            throw new ForbiddenException("Student is not registered for this event");
        }

        EventAttendance existing = eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId).orElse(null);
        if (existing != null) {
            return attendanceMapper.toDto(existing);
        }

        User organizer = userRepository.findByEmail(organizerEmail).orElseThrow();

        EventAttendance attendance = eventAttendanceFactoryMapper.create(AttendanceMethod.ADMIN_SCANNED_STUDENT_QR);
        attendance.setEvent(event);
        attendance.setUser(student);
        attendance.setCheckedInBy(organizer);

        return attendanceMapper.toDto(eventAttendanceRepository.save(attendance));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<AttendanceRecordDto> listAttendance(UUID eventId, String organizerEmail) {
        Event event = getEvent(eventId);
        clubAuthorizationService.requirePlatformAdminOrClubAdminOrStaff(organizerEmail, event.getClub().getId());
        return eventAttendanceRepository.findByEventIdWithUserAndEvent(eventId)
                .stream()
                .map(attendanceMapper::toDto)
                .collect(Collectors.toList());
    }

    public boolean hasAttended(UUID eventId, UUID studentId) {
        return eventAttendanceRepository.findByEventIdAndUserId(eventId, studentId).isPresent();
    }

    private void validateTimeWindow(Event event, EventAttendanceWindow window) {
        if (event.getStartDate() == null) {
            throw new ForbiddenException("Event start time is not configured");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime openAt = event.getStartDate().minusMinutes(window.getOpensBeforeStartMinutes());
        LocalDateTime closeAt = event.getStartDate().plusMinutes(window.getClosesAfterStartMinutes());

        if (now.isBefore(openAt) || now.isAfter(closeAt)) {
            throw new ForbiddenException("Attendance check-in is not allowed at this time");
        }

        // Optionally ensure closeAt does not exceed event endDate if present
        if (event.getEndDate() != null && closeAt.isAfter(event.getEndDate().plusMinutes(5))) {
            // small grace in case admins configure slightly beyond
            // still allow if within 5 minutes, otherwise block
            throw new ForbiddenException("Attendance window exceeds event end time");
        }
    }

    private Event getEvent(UUID eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }
}

