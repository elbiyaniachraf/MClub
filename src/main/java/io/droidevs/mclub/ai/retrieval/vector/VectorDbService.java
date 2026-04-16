package io.droidevs.mclub.ai.retrieval.vector;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Vector DB access using native SQL against pgvector.
 *
 * <p>Uses cosine distance operator (<=>). Score is derived as (1 - distance).
 */
@Service
@RequiredArgsConstructor
public class VectorDbService {

    private final NamedParameterJdbcTemplate jdbc;

    public List<VectorSearchResult> search(String embeddingLiteral, int topK, String entityType) {
        // embeddingLiteral must be like: [0.1,0.2,...]
        String sql = """
                select id, entity_type, event_id, club_id, activity_id, comment_id, content,
                       (1 - (embedding <=> cast(:q as vector))) as score
                from ai_embedding_document
                where (:entityType is null or entity_type = :entityType)
                order by embedding <=> cast(:q as vector)
                limit :k
                """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("q", embeddingLiteral);
        params.addValue("k", topK);
        params.addValue("entityType", (entityType == null || entityType.isBlank()) ? null : entityType);

        return jdbc.query(sql, params, (rs, rowNum) -> {
            UUID id = UUID.fromString(rs.getString("id"));
            String type = rs.getString("entity_type");
            UUID entityId = firstNonNullUuid(
                    rs.getString("event_id"),
                    rs.getString("club_id"),
                    rs.getString("activity_id"),
                    rs.getString("comment_id")
            );
            return new VectorSearchResult(id, type, entityId, rs.getString("content"), rs.getDouble("score"));
        });
    }

    private UUID firstNonNullUuid(String... vals) {
        for (String v : vals) {
            if (v != null) return UUID.fromString(v);
        }
        return null;
    }
}

