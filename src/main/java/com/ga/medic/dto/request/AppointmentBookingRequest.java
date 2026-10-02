package com.ga.medic.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentBookingRequest(

        @NotNull(message = "Doctor ID is required")
        @Positive(message = "Doctor ID must be greater than 0")
        @Schema(description = "ID of the doctor to book the appointment with", example = "1")
        Long doctorId,

        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date must be today or in the future")
        @Schema(description = "Date of the appointment", example = "05/10/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate date,

        @NotNull(message = "Start time is required")
        @Schema(description = "Start time of the requested appointment slot", example = "10:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        @Schema(description = "Reason for the appointment", example = "Routine check-up")
        String reason
) {
}