package com.ga.medic.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleAppointmentRequest(

        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date must be today or in the future")
        @JsonFormat(pattern = "dd/MM/yyyy")
        @Schema(description = "New appointment date", example = "05/11/2026")
        LocalDate date,

        @NotNull(message = "Appointment start time is required")
        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "New appointment start time", example = "10:00")
        LocalTime startTime,

        @NotNull(message = "Expected appointment version is required")
        @PositiveOrZero(message = "Expected appointment version must be zero or greater")
        @Schema(description = "Version returned with the appointment being rescheduled", example = "0")
        Long expectedVersion
) {
}
