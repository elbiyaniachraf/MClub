package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.CommentCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/events")
public class WebEventCommentsController {

    @GetMapping("/{eventId}/comments")
    public String eventComments(@PathVariable UUID eventId) {
        return "redirect:/comments/EVENT/" + eventId;
    }

    @PostMapping("/{eventId}/comments")
    @PreAuthorize("hasRole('STUDENT')")
    public String postEventComment(@PathVariable UUID eventId,
                                   @Valid CommentCreateRequest form) {
        // legacy endpoint - handled by generic comments controller/page
        return "redirect:/comments/EVENT/" + eventId;
    }

    @PostMapping("/{eventId}/comments/{parentId}/reply")
    @PreAuthorize("hasRole('STUDENT')")
    public String replyToEventComment(@PathVariable UUID eventId,
                                      @PathVariable UUID parentId,
                                      @Valid CommentCreateRequest form) {
        // legacy endpoint - handled by generic comments controller/page
        return "redirect:/comments/EVENT/" + eventId;
    }
}
