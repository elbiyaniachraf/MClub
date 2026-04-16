package io.droidevs.mclub.ai.retrieval.vector;

import java.util.List;

/** Adapter for embedding providers (OpenAI, Azure OpenAI, local). */
public interface EmbeddingService {

    /** Returns an embedding vector (float) for the given text. */
    List<Double> embed(String text);

    /** Embedding dimensionality. */
    int dimensions();
}

