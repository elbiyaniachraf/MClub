package io.droidevs.mclub.ai.agent;

import io.droidevs.mclub.ai.conversation.*;
import io.droidevs.mclub.ai.rag.LlmClient;
import io.droidevs.mclub.ai.rag.LlmDecision;
import io.droidevs.mclub.ai.rag.PromptBuilder;
import io.droidevs.mclub.ai.rag.RetrievalContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.ai.retrieval.RetrievalService;
import io.droidevs.mclub.ai.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Multi-step agent loop: retrieval -> LLM -> tool -> LLM ... until answer or maxSteps hit. */
@Component
@RequiredArgsConstructor
public class AgentLoopExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopExecutor.class);

    @Qualifier("hybridRetrievalService")
    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;

    /** Execute agent loop for the current session state. */
    public RagResponse run(ConversationSession session, ConversationContext ctx) {
        int maxSteps = 5;
        Set<String> loopGuard = new HashSet<>();

        ConversationSession localSession = session;

        log.debug("AgentLoop start conversationId={} linked={} email={} userId={} messages={}",
                session.conversationId(),
                ctx.linked(),
                ctx.userEmail().orElse(null),
                ctx.userId().orElse(null),
                session.messages() != null ? session.messages().size() : 0);

        for (int step = 0; step < maxSteps; step++) {
            String userMessage = lastUserMessage(localSession);
            log.debug("AgentLoop step={} userMessage='{}'", step, userMessage);

            RetrievalContext retrieved = retrievalService.retrieve(ctx, userMessage);
            log.debug("AgentLoop step={} retrieval done (hasContext={})", step, retrieved != null);

            String prompt = promptBuilder.buildPrompt(localSession, ctx, retrieved);
            log.debug("AgentLoop step={} promptChars={}", step, prompt != null ? prompt.length() : 0);

            LlmDecision decision = llmClient.decide(prompt);
            log.debug("AgentLoop step={} llmDecision toolCallPresent={} answerChars={}",
                    step,
                    decision.toolCall().isPresent(),
                    decision.answer() != null ? decision.answer().length() : 0);

            if (decision.toolCall().isEmpty()) {
                log.debug("AgentLoop finished step={} returning answer", step);
                return RagResponse.of(decision.answer());
            }

            if (!ctx.linked()) {
                log.debug("AgentLoop blocked tool call because ctx.linked=false");
                return RagResponse.of("To perform actions (register, check-in, rate, comment), please link your WhatsApp number to your MClub account first.");
            }

            ToolCall call = decision.toolCall().get();
            log.debug("AgentLoop step={} toolCall name={} args={}", step, call.toolName(), call.arguments());

            String signature = call.toolName() + ":" + call.arguments();
            if (!loopGuard.add(signature)) {
                log.warn("AgentLoop detected repeating tool call signature={} -> stopping", signature);
                return RagResponse.of("I seem to be looping on the same action. Can you clarify your request?");
            }

            var tool = toolRegistry.get(call.toolName());
            log.debug("AgentLoop step={} executing tool={}", step, call.toolName());

            io.droidevs.mclub.ai.tools.ToolResult result;
            try {
                result = tool.execute(call, ctx);
            } catch (Exception e) {
                log.warn("AgentLoop tool execution failed toolName={} error={}", call.toolName(), e.toString());
                result = io.droidevs.mclub.ai.tools.ToolResult.of(
                        "I tried to perform the action ('" + call.toolName() + "') but the server returned an error: " +
                                (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                );
            }

            log.debug("AgentLoop step={} tool result messageChars={}", step, result.humanMessage() != null ? result.humanMessage().length() : 0);

            localSession = appendAssistant(localSession, "Tool[" + call.toolName() + "]: " + result.humanMessage());
        }

        log.warn("AgentLoop hit maxSteps={} conversationId={}", maxSteps, session.conversationId());
        return RagResponse.of("I couldn't complete the request within a safe number of steps. Please rephrase or be more specific.");
    }

    private String lastUserMessage(ConversationSession session) {
        if (session.messages().isEmpty()) return "";
        return session.messages().get(session.messages().size() - 1).content();
    }

    private ConversationSession appendAssistant(ConversationSession session, String text) {
        List<ConversationMessage> copy = new ArrayList<>(session.messages());
        copy.add(new ConversationMessage(ConversationMessage.Role.ASSISTANT, text, Instant.now()));
        return new ConversationSession(session.conversationId(), session.fromPhoneE164(), copy, session.createdAt());
    }
}
