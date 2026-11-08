package com.ga.medic.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Request to wait for one exact doctor appointment slot")
public record AppointmentWaitlistRequest(
        @NotNull(message = "Doctor ID is required")
        @Positive(message = "Doctor ID must be greater than 0")
        @Schema(description = "Doctor profile ID", example = "1")
        Long doctorId,

        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Waitlist date must be today or in the future")
        @JsonFormat(pattern = "dd/MM/yyyy")
        @Schema(description = "Requested date; today is allowed when the requested time is still in the future", example = "27/10/2026")
        LocalDate date,

        @NotNull(message = "Start time is required")
        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "Exact requested start time", example = "10:00")
        LocalTime startTime
) {
}
