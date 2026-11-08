package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ga.medic.enums.AvailableSlotStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "A valid scheduled time slot and its current booking state")
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
        int slotMinutes,

        @Schema(description = "Current slot state", example = "AVAILABLE",
                allowableValues = {"AVAILABLE", "BOOKED", "WAITLIST_OFFERED"})
        AvailableSlotStatusEnum status,

        @Schema(description = "Whether the authenticated patient may join the waitlist for this slot", example = "true")
        boolean canJoinWaitlist,

        @Schema(description = "Whether the authenticated patient has an overlapping appointment", example = "false")
        boolean patientHasOverlappingAppointment,

        @Schema(description = "Whether the authenticated patient already has a WAITING or OFFERED entry for this slot", example = "false")
        boolean patientHasActiveWaitlistEntry
) {
    public AvailableSlotResponse(Long ruleId, LocalDate date, LocalTime startTime, LocalTime endTime, int slotMinutes) {
        this(ruleId, date, startTime, endTime, slotMinutes, AvailableSlotStatusEnum.AVAILABLE, false, false, false);
    }
}
