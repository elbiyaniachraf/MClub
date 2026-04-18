package io.droidevs.mclub.ai.conversation;

import io.droidevs.mclub.ai.link.WhatsAppLinkService;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Maps WhatsApp phone numbers to MClub users.
 *
 * <p>Production: backed by user_whatsapp_link + OTP verification.
 */
@Service
@RequiredArgsConstructor
public class WhatsappIdentityService {

    private final WhatsAppLinkService linkService;
    private final UserRepository userRepository;

    public ConversationContext buildContext(String fromPhoneE164) {
        Optional<UUID> userId = linkService.findUserIdByPhone(fromPhoneE164);
        Optional<String> email = userId.flatMap(id -> userRepository.findById(id)).map(User::getEmail);
        boolean linked = userId.isPresent();
        return ConversationContext.ofUnscoped(fromPhoneE164, userId, email, linked);
    }
}
