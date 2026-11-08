package com.ga.medic.dto.request;

import com.ga.medic.enums.AppointmentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentStatusRequest(

        @NotNull(message = "Appointment status is required")
        @Schema(description = "Outcome of the appointment", allowableValues = {"COMPLETED", "NO_SHOW"}, example = "COMPLETED")
        AppointmentStatusEnum status
) {
}
