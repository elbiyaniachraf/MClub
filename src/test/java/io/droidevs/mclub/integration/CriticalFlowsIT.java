package io.droidevs.mclub.integration;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.service.*;
import io.droidevs.mclub.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Limited-scope end-to-end integration tests for critical business flows.
 * Uses real services + repositories.
 */
@SpringBootTest
@Transactional
class CriticalFlowsIT {

    @Autowired private UserRepository userRepository;
    @Autowired private ClubRepository clubRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private EventRegistrationRepository eventRegistrationRepository;
    @Autowired private EventAttendanceRepository eventAttendanceRepository;

    @Autowired private EventService eventService;
    @Autowired private RegistrationService registrationService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private EventRatingService eventRatingService;
    @Autowired private CommentService commentService;

    @Test
    void studentRegisters_attends_rates_and_comments() {
        // Arrange
        User admin = userRepository.save(User.builder().email("admin_it@mclub.com").fullName("Admin").password("x").role(Role.PLATFORM_ADMIN).build());
        User student = userRepository.save(User.builder().email("student_it@mclub.com").fullName("Student").password("x").role(Role.STUDENT).build());

        Club club = clubRepository.save(Club.builder().name("Club").description("D").createdBy(admin).build());
        // Important: attach membership to the persisted admin entity with an ID
        membershipRepository.save(Membership.builder().club(club).user(admin).role(ClubRole.ADMIN).status(MembershipStatus.APPROVED).build());

        // Create event via service (requires club-scoped authorization). Platform admin passes.
        EventDto create = new EventDto();
        create.setClubId(club.getId());
        create.setTitle("E");
        create.setDescription("D");
        create.setLocation("L");
        create.setStartDate(LocalDateTime.now().minusHours(2));
        create.setEndDate(LocalDateTime.now().minusHours(1));

        EventDto created = eventService.createEvent(create, admin.getEmail());

        // Student registers
        registrationService.register(created.getId(), student.getEmail());

        // Force attendance record (simulate check-in): open window and check-in via organizer path
        AttendanceWindowRequest w = new AttendanceWindowRequest();
        w.setOpensBeforeStartMinutes(180);
        w.setClosesAfterStartMinutes(180);
        attendanceService.openOrUpdateWindow(created.getId(), w, admin.getEmail());
        attendanceService.organizerCheckInStudent(created.getId(), student.getId(), admin.getEmail());

        // Act: rate
        EventRatingRequest rr = new EventRatingRequest();
        rr.setRating(5);
        rr.setComment("great");
        EventRatingDto rated = eventRatingService.rateEvent(created.getId(), rr, student.getEmail());

        // Act: comment
        CommentCreateRequest cr = new CommentCreateRequest();
        cr.setContent("hello");
        var commentDto = commentService.addComment(CommentTargetType.EVENT, created.getId(), cr, student.getEmail());

        // Assert
        assertNotNull(rated);
        assertNotNull(commentDto.getId());
        assertTrue(eventRegistrationRepository.findByUserIdAndEventId(student.getId(), created.getId()).isPresent());
        assertTrue(eventAttendanceRepository.findByEventIdAndUserId(created.getId(), student.getId()).isPresent());
    }
}



