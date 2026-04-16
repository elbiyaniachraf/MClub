package io.droidevs.mclub.ai.retrieval.vector;

import com.fasterxml.jackson.databind.JsonNode;
import io.droidevs.mclub.ai.llm.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** OpenAI-compatible embedding adapter. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mclub.ai.openai", name = "enabled", havingValue = "true")
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final int DIMS = 1536;

    private final OpenAiProperties props;

    @Override
    public List<Double> embed(String text) {
        WebClient client = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", EMBEDDING_MODEL);
        body.put("input", text);

        JsonNode root = client.post()
                .uri("/v1/embeddings")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .block();

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

    @Override
    public int dimensions() {
        return DIMS;
    }
}


