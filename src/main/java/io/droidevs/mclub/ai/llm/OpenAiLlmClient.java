package io.droidevs.mclub.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.droidevs.mclub.ai.rag.LlmClient;
import io.droidevs.mclub.ai.rag.LlmDecision;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.ai.tools.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

/**
 * OpenAI-compatible LLM client using function/tool calling.
 *
 * <p>Enabled via: mclub.ai.openai.enabled=true
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mclub.ai.openai", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final OpenAiProperties props;
    private final ToolSchemaProvider toolSchemaProvider;
    private final ObjectMapper objectMapper;
    private final List<Tool> tools;

    @Override
    public LlmDecision decide(String prompt) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            log.error("OpenAI is enabled but mclub.ai.openai.api-key is empty. Set it via environment variable OPENAI_API_KEY or application.yml");
            return LlmDecision.answer("OpenAI is enabled but the API key is missing. Please configure OPENAI_API_KEY (or mclub.ai.openai.api-key) and restart the app.");
        }

        WebClient client = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("temperature", 0);
        body.put("tools", toolSchemaProvider.buildToolSchemas(tools));
        body.put("tool_choice", "auto");
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You are MClub Assistant. ALWAYS search first to obtain IDs before executing any action tool. If an action needs an id and it is not known, call search_events/search_clubs/search_semantic (or list tools) and ask the user to pick if multiple matches."),
                Map.of("role", "user", "content", prompt)
        ));

        try {
            JsonNode root = postWithRetry(client, body);

            if (root == null) {
                return LlmDecision.answer("I couldn't reach the language model. Please try again.");
            }

            JsonNode choice0 = root.path("choices").path(0);
            JsonNode message = choice0.path("message");

            // tool call path
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                JsonNode tc = toolCalls.get(0);
                String toolName = tc.path("function").path("name").asText(null);
                String argsJson = tc.path("function").path("arguments").asText("{}");

                if (toolName == null || toolName.isBlank()) {
                    return LlmDecision.answer("Model returned an invalid tool call.");
                }

                Map<String, Object> args = parseArgs(argsJson);
                return LlmDecision.tool(new ToolCall(toolName, args));
            }

            // normal answer
            String content = message.path("content").asText("");
            if (content == null || content.isBlank()) {
                content = "I don't have an answer for that yet.";
            }
            return LlmDecision.answer(content);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            log.error("OpenAI /v1/chat/completions returned 401 Unauthorized. Check your OPENAI_API_KEY and that it has access to model='{}'", props.getModel());
            return LlmDecision.answer("OpenAI authentication failed (401). Please verify OPENAI_API_KEY and restart the app.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
            log.warn("OpenAI /v1/chat/completions rate limited (429). Try again in a moment.");
            return LlmDecision.answer("The AI service is rate-limiting requests right now (429). Please wait a few seconds and try again.");
        }
    }

    private JsonNode postWithRetry(WebClient client, Map<String, Object> body) {
        int maxAttempts = 3;
        long baseDelayMs = 400;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return client.post()
                        .uri("/v1/chat/completions")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofMillis(props.getTimeoutMs()))
                        .block();
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
                if (attempt == maxAttempts) throw e;

                long sleep = baseDelayMs * (1L << (attempt - 1));
                // slightly jitter to avoid thundering herd
                sleep += new java.util.Random().nextInt(150);

                log.warn("OpenAI /v1/chat/completions returned 429 (attempt {}/{}). Backing off {}ms", attempt, maxAttempts, sleep);
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArgs(String argsJson) {
        try {
            return objectMapper.readValue(argsJson, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
