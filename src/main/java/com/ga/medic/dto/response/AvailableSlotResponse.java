package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "A single available time slot")
public record AvailableSlotResponse(
        @Schema(description = "Available rule id", example = "1")
        Long ruleId,

        @Schema(description = "Date of the slot", example = "05/10/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate date,

        @Schema(description = "Start time of the slot", example = "10:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @Schema(description = "End time of the slot", example = "10:30")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        @Schema(description = "Duration in minutes", example = "30")
        int slotMinutes
) {
}