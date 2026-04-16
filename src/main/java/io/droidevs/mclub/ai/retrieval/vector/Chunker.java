package io.droidevs.mclub.ai.retrieval.vector;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple chunker by characters (token-approx). 300–800 tokens ~ 1200–3200 chars.
 *
 * <p>Upgrade later to tokenizer-based chunking if needed.
 */
@Component
public class Chunker {

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();

        int maxChars = 2500;
        int overlap = 200;

        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(text.length(), i + maxChars);
            out.add(text.substring(i, end));
            if (end == text.length()) break;
            i = Math.max(0, end - overlap);
        }
        return out;
    }
}

