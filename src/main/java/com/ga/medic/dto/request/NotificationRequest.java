package com.ga.medic.dto.request;

import com.ga.medic.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationRequest(
        @Schema(description = "Notification title", example = "Appointment Scheduled")
        @NotBlank(message = "Title is required")
        String title,

        @Schema(description = "Notification message", example = "Your appointment has been scheduled for tomorrow")
        @NotBlank(message = "Message is required")
        String message,

        @Schema(description = "Type of notification", example = "APPOINTMENT")
        @NotNull(message = "Notification type is required")
        NotificationType type,

        @Schema(description = "ID of related entity (e.g., appointment ID)", example = "123")
        Long relatedEntityId
) {
}

