package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.ClubApplicationDto;
import io.droidevs.mclub.service.ClubApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/club-applications")
@RequiredArgsConstructor
public class WebClubApplicationController {

    private final ClubApplicationService applicationService;

    @PostMapping("/submit")
    public String submitApplication(@ModelAttribute ClubApplicationDto dto, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            applicationService.submitApplication(dto, auth.getName());
            redirectAttributes.addFlashAttribute("message", "Club application submitted successfully and is pending approval!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Application failed: " + e.getMessage());
        }
        return "redirect:/clubs";
    }

    @GetMapping("/apply")
    public String showApplicationForm(Model model) {
        model.addAttribute("clubApplicationDto", new ClubApplicationDto());
        return "apply-club";
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public String viewApplications(Model model) {
        model.addAttribute("applications", applicationService.getPendingApplications());
        return "club-applications";
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public String approve(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            applicationService.approveApplication(id);
            redirectAttributes.addFlashAttribute("message", "Application approved. Club created and user assigned as Admin.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve: " + e.getMessage());
        }
        return "redirect:/club-applications";
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public String reject(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            applicationService.rejectApplication(id);
            redirectAttributes.addFlashAttribute("message", "Application rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject: " + e.getMessage());
        }
        return "redirect:/club-applications";
    }
}
