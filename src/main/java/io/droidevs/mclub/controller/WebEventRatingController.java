package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.EventRatingRequest;
import io.droidevs.mclub.service.EventRatingService;
import io.droidevs.mclub.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/events")
public class WebEventRatingController {

    private final EventService eventService;
    private final EventRatingService eventRatingService;

    @GetMapping("/{eventId}/rate")
    @PreAuthorize("hasRole('STUDENT')")
    public String ratePage(@PathVariable UUID eventId, Model model, Authentication auth) {
        model.addAttribute("event", eventService.getEvent(eventId));

        // helpful to prefill if user already rated
        try {
            model.addAttribute("myRating", eventRatingService.getMyRating(eventId, auth.getName()));
        } catch (Exception ignored) {
            // no rating yet
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new EventRatingRequest());
        }

        return "event-rate";
    }

    @PostMapping("/{eventId}/rate")
    @PreAuthorize("hasRole('STUDENT')")
    public String submitRating(@PathVariable UUID eventId,
                               @Valid @ModelAttribute("form") EventRatingRequest form,
                               Authentication auth,
                               RedirectAttributes ra) {
        try {
            eventRatingService.rateEvent(eventId, form, auth.getName());
            ra.addFlashAttribute("message", "Thanks! Your rating was saved.");
            return "redirect:/events/" + eventId + "/rate";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
            return "redirect:/events/" + eventId + "/rate";
        }
    }
}

