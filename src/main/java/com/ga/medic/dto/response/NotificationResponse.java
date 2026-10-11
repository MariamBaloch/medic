package com.ga.medic.dto.response;

import com.ga.medic.enums.NotificationAction;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationResponse(
        @Schema(description = "The identifier of the notification", example = "1")
        Long id,

        @Schema(description = "The title of the notification", example = "New Appointment Scheduled")
        String title,

        @Schema(description = "The message of the notification", example = "You have a new appointment scheduled at 10:00 AM.")
        String message,

        @Schema(description = "Type of entity the notification is related to", example = "APPOINTMENT")
        String type,

        @Schema(description = "Action that triggered the notification", example = "CANCELLED")
        NotificationAction action,

        @Schema(description = "The identifier of the related entity (e.g., appointment)", example = "42")
        Long relatedEntityId,

        @Schema(description = "Whether the notification has been read", example = "false")
        boolean read
) {
}