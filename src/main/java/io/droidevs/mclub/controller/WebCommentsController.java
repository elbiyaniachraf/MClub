package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.CommentTargetType;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.service.CommentService;
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
@RequestMapping("/comments")
public class WebCommentsController {

    private final CommentService commentService;
    private final EventService eventService;

    @GetMapping("/{targetType}/{targetId}")
    public String comments(@PathVariable String targetType,
                           @PathVariable UUID targetId,
                           Model model,
                           Authentication auth) {
        CommentTargetType type = CommentTargetType.valueOf(targetType.toUpperCase());

        // For now, we have a web detail page only for events.
        // Activities are supported as a comment target in the backend API, but UI routing to an activity page
        // can be wired once an activity detail page exists.
        if (type == CommentTargetType.EVENT) {
            Event event = eventService.getEvent(targetId);
            model.addAttribute("event", event);
            model.addAttribute("backUrl", "/events/" + event.getId());
            model.addAttribute("pageTitle", "Comments");
            model.addAttribute("pageSubtitle", event.getTitle());
        } else {
            model.addAttribute("backUrl", "/");
            model.addAttribute("pageTitle", "Comments");
            model.addAttribute("pageSubtitle", "Activity");
        }

        model.addAttribute("targetType", type.name());
        model.addAttribute("targetId", targetId);
        model.addAttribute("comments", commentService.getThread(type, targetId, auth != null ? auth.getName() : null));
        model.addAttribute("form", new CommentCreateRequest());
        return "comments";
    }

    @PostMapping("/{targetType}/{targetId}")
    @PreAuthorize("hasRole('STUDENT')")
    public String postComment(@PathVariable String targetType,
                              @PathVariable UUID targetId,
                              @Valid CommentCreateRequest form,
                              Authentication auth,
                              RedirectAttributes ra) {
        CommentTargetType type = CommentTargetType.valueOf(targetType.toUpperCase());
        try {
            form.setParentId(null);
            commentService.addComment(type, targetId, form, auth.getName());
            ra.addFlashAttribute("message", "Comment posted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not post comment: " + e.getMessage());
        }
        return "redirect:/comments/" + type.name() + "/" + targetId;
    }

    @PostMapping("/{targetType}/{targetId}/{parentId}/reply")
    @PreAuthorize("hasRole('STUDENT')")
    public String reply(@PathVariable String targetType,
                        @PathVariable UUID targetId,
                        @PathVariable UUID parentId,
                        @Valid CommentCreateRequest form,
                        Authentication auth,
                        RedirectAttributes ra) {
        CommentTargetType type = CommentTargetType.valueOf(targetType.toUpperCase());
        try {
            form.setParentId(parentId);
            commentService.addComment(type, targetId, form, auth.getName());
            ra.addFlashAttribute("message", "Reply posted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not post reply: " + e.getMessage());
        }
        return "redirect:/comments/" + type.name() + "/" + targetId;
    }
}

