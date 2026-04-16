package io.droidevs.mclub.ai.conversation;

import java.util.Optional;
import java.util.UUID;

/** Derived context for a conversation (identity + permissions). */
public record ConversationContext(
        String fromPhoneE164,
        Optional<UUID> userId,
        Optional<String> userEmail,
        boolean linked
) {}


