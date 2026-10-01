package com.ga.medic.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record AvailabilityRuleRequest(

        @NotNull(message = "Day of week is required")
        @Schema(description = "Days of the week when the doctor is available", example = "[\"MONDAY\", \"TUESDAY\"]")
        Set<DayOfWeek> daysOfWeek,

        @NotNull(message = "Start time is required")
        @Schema(description = "Availability start time", example = "09:00")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        @Schema(description = "Availability end time", example = "12:00")
        LocalTime endTime,

        @NotNull(message = "Slot duration is required")
        @Schema(description = "Duration of each appointment slot in minutes", example = "30")
        Integer slotMinutes,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must not be in the past")
        @JsonFormat(pattern = "dd/MM/yyyy")
        @Schema(description = "Date when the availability starts", example = "01/10/2026")
        LocalDate startDate,

        @JsonFormat(pattern = "dd/MM/yyyy")
        @Schema(description = "Date when the availability ends. Leave empty for no end date", example = "31/10/2026")
        LocalDate endDate

) {
    @AssertTrue(message = "End time must be after start time")
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) return true;
        return endTime.isAfter(startTime);
    }

    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        if (endDate == null || startDate == null) return true;
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "Slot duration must be either 15 or 30 minutes")
    public boolean isValidSlotMinutes() {
        return slotMinutes == null || slotMinutes == 15 || slotMinutes == 30;
    }

}