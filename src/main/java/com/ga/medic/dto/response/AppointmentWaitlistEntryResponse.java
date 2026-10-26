package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ga.medic.enums.WaitlistStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "The authenticated patient's waitlist entry")
public record AppointmentWaitlistEntryResponse(
        Long id,
        Long doctorId,
        String doctorName,
        @JsonFormat(pattern = "dd/MM/yyyy") LocalDate appointmentDate,
        @JsonFormat(pattern = "HH:mm") LocalTime preferredStartTime,
        WaitlistStatusEnum status,
        Instant offeredAt,
        Instant offerExpiresAt
) {
}
