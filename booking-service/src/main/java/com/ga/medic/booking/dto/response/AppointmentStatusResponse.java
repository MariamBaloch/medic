package com.ga.medic.booking.dto.response;

import com.ga.medic.booking.enums.AppointmentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Appointment status option")
public record AppointmentStatusResponse(

        @Schema(description = "Status value", example = "BOOKED")
        AppointmentStatusEnum value,

        @Schema(description = "Human-readable status label", example = "Booked")
        String label
) {
}
