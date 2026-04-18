package io.droidevs.mclub.ai.retrieval.vector;

import com.fasterxml.jackson.databind.JsonNode;
import io.droidevs.mclub.ai.llm.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OpenAI-compatible embedding adapter. */
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mclub.ai.openai", name = "enabled", havingValue = "true")
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final int DIMS = 1536;

    private final OpenAiProperties props;

    @Override
    public List<Double> embed(String text) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            log.error("OpenAI embeddings are enabled but mclub.ai.openai.api-key is empty. Set OPENAI_API_KEY and restart.");
            throw new IllegalStateException("OpenAI API key is missing (OPENAI_API_KEY / mclub.ai.openai.api-key)");
        }

        WebClient client = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", EMBEDDING_MODEL);
        body.put("input", text);

        JsonNode root;
        try {
            root = postEmbeddingWithRetry(client, body);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            log.error("OpenAI /v1/embeddings returned 401 Unauthorized. Check OPENAI_API_KEY.");
            throw e;
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
            log.warn("OpenAI /v1/embeddings rate limited (429). Try again in a moment.");
            throw e;
        }

        if (root == null) {
            throw new IllegalStateException("Embedding provider returned empty response");
        }

        JsonNode embedding = root.path("data").path(0).path("embedding");
        if (!embedding.isArray()) {
            throw new IllegalStateException("Invalid embedding response format");
        }

        List<Double> out = new ArrayList<>(embedding.size());
        for (JsonNode v : embedding) {
            out.add(v.asDouble());
        }
        return out;
    }

    private JsonNode postEmbeddingWithRetry(WebClient client, Map<String, Object> body) {
        int maxAttempts = 3;
        long baseDelayMs = 400;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return client.post()
                        .uri("/v1/embeddings")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofMillis(props.getTimeoutMs()))
                        .block();
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
                if (attempt == maxAttempts) throw e;

                long sleep = baseDelayMs * (1L << (attempt - 1));
                sleep += new java.util.Random().nextInt(150);

                log.warn("OpenAI /v1/embeddings returned 429 (attempt {}/{}). Backing off {}ms", attempt, maxAttempts, sleep);
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

    @Override
    public int dimensions() {
        return DIMS;
    }
}
