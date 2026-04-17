package io.droidevs.mclub.ai.retrieval.vector;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Dev/test embedding implementation.
 *
 * <p>This keeps the application context bootable when no real embedding provider (OpenAI/Azure/etc)
 * is configured. It is NOT suitable for production semantic search quality.
 */
@Service
@Primary
public class InMemoryEmbeddingService implements EmbeddingService {

    private static final int DIMS = 32;

    @Override
    public List<Double> embed(String text) {
        byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        double[] acc = new double[DIMS];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            int idx = i % DIMS;
            acc[idx] += (b / 255.0);
        }
        List<Double> out = new ArrayList<>(DIMS);
        for (double v : acc) {
            out.add(v);
        }
        return out;
    }

    @Override
    public int dimensions() {
        return DIMS;
    }
}
