package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

@Schema(description = "A continuous range containing one or more available appointment start times")
public record AvailableTimeRangeResponse(
        @Schema(description = "Start of the open range, in 24-hour time", example = "13:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @Schema(description = "End of the open range, in 24-hour time", example = "14:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        @Schema(description = "Duration of each bookable slot in this range", example = "30")
        int slotMinutes,

        @Schema(description = "Human-readable time range", example = "1:00 PM - 2:00 PM")
        String displayRange
) {
}
