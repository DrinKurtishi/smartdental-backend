package com.smartdental.dto.audit;

import com.smartdental.entity.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String actorName,
        String action,
        String entityType,
        String entityId,
        String details,
        Instant createdAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor() != null ? log.getActor().getFullName() : "SYSTEM",
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getCreatedAt());
    }
}
