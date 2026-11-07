package com.ledgerline.dto;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Long actorUserId,
        String action,
        String entityType,
        String entityId,
        String requestId,
        Instant createdAt) {
}
