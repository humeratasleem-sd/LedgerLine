package com.ledgerline.service;

import java.util.List;

import com.ledgerline.dto.AuditEventResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {

    private final JdbcTemplate jdbcTemplate;

    public AuditEventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> latestEvents(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("The audit event limit must be between 1 and 100.");
        }
        return jdbcTemplate.query("""
                SELECT id, actor_user_id, action, entity_type, entity_id, request_id, created_at
                FROM audit_events
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """, (resultSet, rowNumber) -> {
            long actorId = resultSet.getLong("actor_user_id");
            Long nullableActorId = resultSet.wasNull() ? null : actorId;
            return new AuditEventResponse(
                    resultSet.getLong("id"),
                    nullableActorId,
                    resultSet.getString("action"),
                    resultSet.getString("entity_type"),
                    resultSet.getString("entity_id"),
                    resultSet.getString("request_id"),
                    resultSet.getTimestamp("created_at").toInstant());
        }, limit);
    }
}
