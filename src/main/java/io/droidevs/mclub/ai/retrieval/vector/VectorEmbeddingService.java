package io.droidevs.mclub.ai.retrieval.vector;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Backwards-compatible wrapper around {@link EmbeddingService}.
 *
 * <p>Kept to avoid breaking previous wiring; new code should depend on {@link EmbeddingService}.
 */
@Service
public class VectorEmbeddingService {

    private final EmbeddingService delegate;

    public VectorEmbeddingService(EmbeddingService delegate) {
        this.delegate = delegate;
    }

    public List<Double> embed(String text) {
        return delegate.embed(text);
    }
}

