package com.ga.medic.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityExceptionRequest(

        @NotNull(message = "Exception date is required")
        @FutureOrPresent(message = "Exception date must be today or in the future")
        @Schema(description = "Date on which the doctor's availability is modified", example = "01/10/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate exceptionDate,

        @Schema(description = "Start time of the exception. Leave null for a full-day exception.", example = "09:00", nullable = true)
        LocalTime startTime,

        @Schema(description = "End time of the exception. Leave null for a full-day exception.", example = "12:00", nullable = true)
        LocalTime endTime,

        @Schema(description = "Reason for the availability exception", example = "Doctor has a medical appointment")
        String reason

) {

    @AssertTrue(message = "Start time and end time must both be provided or both be null")
    @JsonIgnore
    public boolean isTimeRangeValid() {
        return (startTime == null && endTime == null) || (startTime != null && endTime != null);
    }

    @AssertTrue(message = "End time must be after start time")
    @JsonIgnore
    public boolean isTimeOrderValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}