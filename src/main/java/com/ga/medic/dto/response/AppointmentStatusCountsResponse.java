package com.ga.medic.dto.response;

import com.ga.medic.enums.AppointmentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Total appointments and counts grouped by status")
public record AppointmentStatusCountsResponse(

        @Schema(description = "Total number of matching appointments", example = "34")
        long total,

        @Schema(description = "Count for every appointment status, including statuses with zero appointments")
        Map<AppointmentStatusEnum, Long> byStatus
) {
}
