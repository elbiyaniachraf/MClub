package io.droidevs.mclub.ai.retrieval.vector;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ingestion pipeline: entity -> text -> chunks -> embedding -> vector table.
 *
 * <p>Does not bypass domain services; callers should provide already-authorized, safe text.
 */
@Service
@RequiredArgsConstructor
public class VectorIngestionService {

    private final NamedParameterJdbcTemplate jdbc;
    private final EmbeddingService embeddingService;
    private final TextNormalizer normalizer;
    private final Chunker chunker;

    public void reindexEvent(UUID eventId, String safeText, String metadataJson) {
        upsert("EVENT", eventId, null, null, null, safeText, metadataJson);
    }

    public void reindexClub(UUID clubId, String safeText, String metadataJson) {
        upsert("CLUB", null, clubId, null, null, safeText, metadataJson);
    }

    public void reindexActivity(UUID activityId, String safeText, String metadataJson) {
        upsert("ACTIVITY", null, null, activityId, null, safeText, metadataJson);
    }

    public void reindexComment(UUID commentId, String safeText, String metadataJson) {
        upsert("COMMENT", null, null, null, commentId, safeText, metadataJson);
    }

    private void upsert(String type, UUID eventId, UUID clubId, UUID activityId, UUID commentId, String text, String metadataJson) {
        String normalized = normalizer.normalize(text);
        List<String> chunks = chunker.chunk(normalized);

        // Delete existing chunks for this entity reference (idempotent reindex)
        deleteExisting(type, eventId, clubId, activityId, commentId);

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            List<Double> emb = embeddingService.embed(chunk);
            insertRow(type, eventId, clubId, activityId, commentId, i, chunk, metadataJson, VectorSearchService.toPgvectorLiteral(emb));
        }
    }

    private void deleteExisting(String type, UUID eventId, UUID clubId, UUID activityId, UUID commentId) {
        String sql = """
                delete from ai_embedding_document
                where entity_type = :t
                  and ((:eventId is not null and event_id = :eventId)
                    or (:clubId is not null and club_id = :clubId)
                    or (:activityId is not null and activity_id = :activityId)
                    or (:commentId is not null and comment_id = :commentId))
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("t", type)
                .addValue("eventId", eventId)
                .addValue("clubId", clubId)
                .addValue("activityId", activityId)
                .addValue("commentId", commentId));
    }

    private void insertRow(String type, UUID eventId, UUID clubId, UUID activityId, UUID commentId,
                           int chunkIndex, String content, String metadataJson, String embeddingLiteral) {
        String sql = """
                insert into ai_embedding_document (
                    id, entity_type, event_id, club_id, activity_id, comment_id,
                    chunk_index, content, metadata, embedding, created_at, updated_at
                ) values (
                    :id, :t, :eventId, :clubId, :activityId, :commentId,
                    :chunkIndex, :content, cast(:metadata as jsonb), cast(:embedding as vector), :now, :now
                )
                """;

        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("t", type)
                .addValue("eventId", eventId)
                .addValue("clubId", clubId)
                .addValue("activityId", activityId)
                .addValue("commentId", commentId)
                .addValue("chunkIndex", chunkIndex)
                .addValue("content", content)
                .addValue("metadata", metadataJson == null ? "{}" : metadataJson)
                .addValue("embedding", embeddingLiteral)
                .addValue("now", Instant.now()));
    }
}


