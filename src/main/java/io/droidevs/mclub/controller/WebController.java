package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.domain.MembershipStatus;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.ActivityCreateRequest;
import io.droidevs.mclub.dto.EventCreateRequest;
import io.droidevs.mclub.dto.AttendanceRecordDto;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.service.CurrentUserService;
import io.droidevs.mclub.service.AttendanceService;
import io.droidevs.mclub.service.ClubService;
import io.droidevs.mclub.service.EventService;
import io.droidevs.mclub.service.MembershipService;
import io.droidevs.mclub.service.EventRatingService;
import io.droidevs.mclub.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ClubService clubService;
    private final EventService eventService;
    private final MembershipService membershipService;
    private final AttendanceService attendanceService;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final CurrentUserService currentUserService;
    private final EventRatingService eventRatingService;
    private final ActivityService activityService;

    @GetMapping("/")
    public String dashboard(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        // Pass info about current user
        model.addAttribute("username", auth.getName());
        model.addAttribute("authorities", auth.getAuthorities());

        // Load partial data for dashboard
        model.addAttribute("recentClubs", clubService.getAllClubs(PageRequest.of(0, 5)).getContent());
        model.addAttribute("recentEvents", eventService.getAllEvents(PageRequest.of(0, 5)).getContent());

        return "dashboard";
    }

    @GetMapping("/clubs")
    public String clubs(Model model, Pageable pageable) {
        model.addAttribute("clubsPage", clubService.getAllClubs(pageable));
        return "clubs";
    }

    @GetMapping("/clubs/{id}")
    public String clubDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("club", clubService.getClub(id));
        model.addAttribute("members", membershipService.getApprovedMembers(id));

        // snapshots
        model.addAttribute("recentEvents", eventService.getRecentEventsByClub(id));
        model.addAttribute("recentActivities", activityService.getRecentByClub(id));

        return "club-detail";
    }

    @GetMapping("/clubs/{id}/members")
    public String clubMembers(@PathVariable UUID id, Model model) {
        model.addAttribute("club", clubService.getClub(id));
        model.addAttribute("members", membershipService.getApprovedMembers(id));
        return "club-members";
    }

    @GetMapping("/clubs/{id}/events")
    public String clubEvents(@PathVariable UUID id, Model model) {
        model.addAttribute("club", clubService.getClub(id));
        model.addAttribute("events", eventService.getEventsByClub(id));
        return "club-events";
    }

    @GetMapping("/clubs/{id}/activities")
    public String clubActivities(@PathVariable UUID id, Model model) {
        model.addAttribute("club", clubService.getClub(id));
        model.addAttribute("activities", activityService.getByClub(id));
        return "club-activities";
    }

    @GetMapping("/events")
    public String events(Model model, Pageable pageable) {
        model.addAttribute("eventsPage", eventService.getAllEvents(pageable));
        return "events";
    }

    @GetMapping("/events/{id}")
    public String eventDetail(@PathVariable UUID id, Model model, Authentication auth) {
        var event = eventService.getEvent(id);

        boolean eventEnded = event.getEndDate() != null
                ? event.getEndDate().isBefore(LocalDateTime.now())
                : (event.getStartDate() != null && event.getStartDate().isBefore(LocalDateTime.now()));

        List<AttendanceRecordDto> attendance = Collections.emptyList();
        if (eventEnded && auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                attendance = attendanceService.listAttendance(id, auth.getName());
            } catch (Exception ignored) {
                // keep empty
            }
        }

        // Rating summary (public endpoint; safe for anonymous users)
        double ratingAverage = 0.0;
        long ratingCount = 0L;
        try {
            var summary = eventRatingService.getSummary(id);
            ratingAverage = summary.getAverage();
            ratingCount = summary.getCount();
        } catch (Exception ignored) {
        }

        model.addAttribute("event", event);
        model.addAttribute("eventEnded", eventEnded);
        model.addAttribute("attendance", attendance);
        model.addAttribute("attendanceCount", attendance.size());
        model.addAttribute("ratingAverage", ratingAverage);
        model.addAttribute("ratingCount", ratingCount);
        model.addAttribute("auth", auth);
        return "event-detail";
    }

    @GetMapping("/club-admin/clubs/{clubId}/events/new")
    public String newClubEvent(@PathVariable UUID clubId, Model model, Authentication auth) {
        User user = currentUserService.requireUser(auth);
        if (membershipRepository.findByUserIdAndClubId(user.getId(), clubId)
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                .map(m -> m.getRole() == ClubRole.ADMIN || m.getRole() == ClubRole.STAFF)
                .orElse(false) == false) {
            return "redirect:/";
        }
        var club = clubRepository.findById(clubId).orElseThrow();
        EventCreateRequest form = new EventCreateRequest();
        form.setClubId(clubId);
        model.addAttribute("club", club);
        model.addAttribute("form", form);
        return "club-event-new";
    }

    @GetMapping("/club-admin/clubs/{clubId}/activities/new")
    public String newClubActivity(@PathVariable UUID clubId, Model model, Authentication auth) {
        User user = currentUserService.requireUser(auth);
        if (membershipRepository.findByUserIdAndClubId(user.getId(), clubId)
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                .map(m -> m.getRole() == ClubRole.ADMIN || m.getRole() == ClubRole.STAFF)
                .orElse(false) == false) {
            return "redirect:/";
        }
        var club = clubRepository.findById(clubId).orElseThrow();
        ActivityCreateRequest form = new ActivityCreateRequest();
        form.setClubId(clubId);
        model.addAttribute("club", club);
        model.addAttribute("form", form);
        model.addAttribute("events", eventService.getEventsByClub(clubId));
        return "club-activity-new";
    }
}
