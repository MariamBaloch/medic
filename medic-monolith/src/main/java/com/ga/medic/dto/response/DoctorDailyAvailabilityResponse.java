package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "A doctor's open time ranges for one date")
public record DoctorDailyAvailabilityResponse(
        @Schema(description = "Doctor profile ID", example = "12")
        Long doctorId,

        @Schema(description = "Date of the availability", example = "25/10/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate date,

        @Schema(description = "Contiguous ranges of available appointment slots")
        List<AvailableTimeRangeResponse> availableRanges
) {
}
