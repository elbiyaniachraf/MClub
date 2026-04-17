package io.droidevs.mclub.ai.llm;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Spring config for OpenAI-compatible LLM access. */
@Data
@Validated
@ConfigurationProperties(prefix = "mclub.ai.openai")
public class OpenAiProperties {

    /** Set to true to enable OpenAI client instead of StubLlmClient. */
    private boolean enabled = false;

    /** Base URL for OpenAI-compatible API, e.g. https://api.openai.com */
    @NotBlank // safe default; always present
    private String baseUrl = "https://api.openai.com";

    /** API key for Authorization: Bearer ... (required only when enabled). */
    private String apiKey;

    /** Model name, e.g. gpt-4.1-mini, gpt-4o-mini, etc. (required only when enabled). */
    private String model = "gpt-4o-mini";

    /** Request timeout in milliseconds. */
    private long timeoutMs = 15000;

}
