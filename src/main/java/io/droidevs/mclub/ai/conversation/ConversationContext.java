package io.droidevs.mclub.ai.conversation;

import java.util.Optional;
import java.util.UUID;

/** Derived context for a conversation (identity + permissions + optional scope). */
public record ConversationContext(
        String fromPhoneE164,
        Optional<UUID> userId,
        Optional<String> userEmail,
        boolean linked,
        Optional<UUID> clubScopeId,
        Optional<UUID> eventScopeId
) {
    public static ConversationContext ofUnscoped(String fromPhoneE164,
                                                Optional<UUID> userId,
                                                Optional<String> userEmail,
                                                boolean linked) {
        return new ConversationContext(fromPhoneE164, userId, userEmail, linked, Optional.empty(), Optional.empty());
    }
}
