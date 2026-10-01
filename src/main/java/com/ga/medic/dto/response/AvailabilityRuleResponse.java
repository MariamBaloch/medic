package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record AvailabilityRuleResponse(

        @Schema(description = "Rule ID", example = "1")
        Long id,

        @Schema(description = "Start date of the rule", example = "01/10/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate startDate,

        @Schema(description = "End date of the rule", example = "31/12/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate endDate,

        @Schema(description = "Days of the week", example = "[\"MONDAY\", \"WEDNESDAY\", \"FRIDAY\"]")
        Set<DayOfWeek> daysOfWeek,

        @Schema(description = "Start time", example = "09:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @Schema(description = "End time", example = "17:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        @Schema(description = "Slot duration in minutes", example = "30")
        int slotMinutes,

        AuditResponse audit
) {
}