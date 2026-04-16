package io.droidevs.mclub.ai.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Request from platform chat widget (web/mobile). */
public record ChatMessageRequest(
        String conversationId,
        @NotBlank String from,
        @NotBlank String text
) {}

