package com.ga.medic.notification.dto;

import com.ga.medic.notification.enums.NotificationAction;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationResponse(
        @Schema(description = "The identifier of the notification", example = "1")
        Long id,
        @Schema(description = "The title of the notification")
        String title,
        @Schema(description = "The message of the notification")
        String message,
        @Schema(description = "Type of entity the notification is related to", example = "APPOINTMENT")
        String type,
        @Schema(description = "Action that triggered the notification")
        NotificationAction action,
        @Schema(description = "Related entity id")
        Long relatedEntityId,
        @Schema(description = "Whether the notification has been read")
        boolean read
) {
}
