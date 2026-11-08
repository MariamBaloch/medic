package com.ga.medic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(

        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 255, message = "Cancellation reason cannot exceed 255 characters")
        @Schema(description = "Reason for cancelling the appointment", example = "Patient is unavailable at the scheduled time")
        String reason
) {
}