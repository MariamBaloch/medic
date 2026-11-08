package com.ga.medic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record AuditResponse(
        @Schema(description = "Created at", example = "2023-01-01T00:00:00Z")
        Instant createdAt,
        @Schema(description = "Updated at", example = "2023-01-01T00:00:00Z")
        Instant updatedAt
) {
}
