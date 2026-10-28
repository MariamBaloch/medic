package com.ga.medic.dto.response;

import com.ga.medic.enums.AppointmentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Appointment status option")
public record AppointmentStatusResponse(

        @Schema(description = "Status value", example = "BOOKED")
        AppointmentStatusEnum value,

        @Schema(description = "Human-readable status label", example = "Booked")
        String label
) {
}
