package io.droidevs.mclub.ai.retrieval.vector;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Repository for vector similarity search + filters. */
@Repository
public class VectorIndexRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public VectorIndexRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<VectorSearchResult> search(String embeddingLiteral, int topK, String entityType, UUID clubId, UUID eventId) {
        StringBuilder sql = new StringBuilder();
        sql.append("""
                select id, entity_type, event_id, club_id, activity_id, comment_id, content,
                       (1 - (embedding <=> cast(:q as vector))) as score
                from ai_embedding_document
                where 1=1
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("q", embeddingLiteral);
        params.addValue("k", topK);

        if (entityType != null && !entityType.isBlank()) {
            sql.append(" and entity_type = :entityType");
            params.addValue("entityType", entityType);
        }
        if (clubId != null) {
            sql.append(" and club_id = :clubId");
            params.addValue("clubId", clubId);
        }
        if (eventId != null) {
            sql.append(" and event_id = :eventId");
            params.addValue("eventId", eventId);
        }

        sql.append(" order by embedding <=> cast(:q as vector) limit :k");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
            UUID id = UUID.fromString(rs.getString("id"));
            String type = rs.getString("entity_type");
            UUID sourceId = firstNonNullUuid(
                    rs.getString("event_id"),
                    rs.getString("club_id"),
                    rs.getString("activity_id"),
                    rs.getString("comment_id")
            );
            return new VectorSearchResult(id, type, sourceId, rs.getString("content"), rs.getDouble("score"));
        });
    }

    private UUID firstNonNullUuid(String... vals) {
        for (String v : vals) {
            if (v != null) return UUID.fromString(v);
        }
        return null;
    }
}

