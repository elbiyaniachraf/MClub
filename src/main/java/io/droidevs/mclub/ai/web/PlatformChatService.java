package io.droidevs.mclub.ai.web;

import io.droidevs.mclub.ai.conversation.*;
import io.droidevs.mclub.ai.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Synchronous chat entry point for in-platform web/mobile chat.
 *
 * <p>Does NOT send via WhatsAppSender. Returns response immediately.
 */
@Service
@RequiredArgsConstructor
public class PlatformChatService {

    private final ConversationStore store;
    private final RagService ragService;
    private final io.droidevs.mclub.repository.UserRepository userRepository;

    public RagResponse chat(String conversationId, String from, String text) {
        ConversationSession session = store.getOrCreate(conversationId, from);
        session = store.appendUserMessage(session, text);

        // If the user is authenticated in the platform session, use that identity for authorization.
        ConversationContext ctx = buildContext(from);
        RagResponse response = ragService.handle(session, ctx);

        store.appendAssistantMessage(session, response.message());
        return response;
    }

    private ConversationContext buildContext(String from) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return new ConversationContext(from, java.util.Optional.empty(), java.util.Optional.empty(), false);
        }

        // In this project the principal username is the email (e.g. Jwt or form login).
        String email = auth.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return new ConversationContext(from, java.util.Optional.empty(), java.util.Optional.empty(), false);
        }

        var userId = userRepository.findByEmail(email).map(io.droidevs.mclub.domain.User::getId);
        boolean linked = userId.isPresent();
        return new ConversationContext(from, userId, java.util.Optional.of(email), linked);
    }
}
