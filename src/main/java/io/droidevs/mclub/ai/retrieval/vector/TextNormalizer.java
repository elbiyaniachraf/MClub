package io.droidevs.mclub.ai.retrieval.vector;

import org.springframework.stereotype.Component;

/** Minimal text normalization for embeddings (PII-safe). */
@Component
public class TextNormalizer {

    public String normalize(String input) {
        if (input == null) return "";
        String s = input.strip();
        // Collapse whitespace
        s = s.replaceAll("\\s+", " ");
        return s;
    }
}

