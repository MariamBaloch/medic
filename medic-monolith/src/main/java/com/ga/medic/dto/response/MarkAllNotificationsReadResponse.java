package com.ga.medic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record MarkAllNotificationsReadResponse(
        @Schema(description = "Number of notifications changed to read", example = "3")
        int updatedCount
) {
}
