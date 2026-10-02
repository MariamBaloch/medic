package com.ga.medic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Doctor's calendar view with available slots and booked appointments")
public record DoctorCalendarResponse(

        @Schema(description = "Computed available slots")
        List<AvailableSlotResponse> availableSlots,

        @Schema(description = "Existing non-cancelled appointments")
        List<AppointmentResponse> appointments
) {
}