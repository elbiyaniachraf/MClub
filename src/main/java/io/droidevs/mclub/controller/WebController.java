package io.droidevs.mclub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final io.droidevs.mclub.service.ClubService clubService;
    private final io.droidevs.mclub.service.EventService eventService;

    @GetMapping("/")
    public String dashboard(Model model, org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }

        // Pass info about current user
        model.addAttribute("username", auth.getName());
        model.addAttribute("authorities", auth.getAuthorities());

        // Load partial data for dashboard
        model.addAttribute("recentClubs", clubService.getAllClubs(org.springframework.data.domain.PageRequest.of(0, 5)).getContent());
        model.addAttribute("recentEvents", eventService.getAllEvents(org.springframework.data.domain.PageRequest.of(0, 5)).getContent());

        return "dashboard";
    }

    @GetMapping("/clubs")
    public String clubs(Model model, org.springframework.data.domain.Pageable pageable) {
        model.addAttribute("clubsPage", clubService.getAllClubs(pageable));
        return "clubs";
    }

    @GetMapping("/events")
    public String events(Model model, org.springframework.data.domain.Pageable pageable) {
        model.addAttribute("eventsPage", eventService.getAllEvents(pageable));
        return "events";
    }
}
