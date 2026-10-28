package com.ga.medic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnreadNotificationCountResponse(
        @Schema(description = "Number of unread notifications for the authenticated user", example = "3")
        long unreadCount
) {
}
