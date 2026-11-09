package com.ga.medic.audit.dto;

import com.ga.medic.audit.enums.AuditAction;
import com.ga.medic.audit.enums.AuditEntityType;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String username,
        AuditAction action,
        AuditEntityType entityType,
        String description,
        LocalDateTime timestamp
) {
}
