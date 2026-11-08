package com.ga.medic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Doctor's scheduled slots and the authenticated patient's appointments in the requested date range")
public record DoctorAvailabilityResponse(
        @Schema(description = "Valid scheduled slots with current booking and waitlist state")
        List<AvailableSlotResponse> slots,

        @Schema(description = "The authenticated patient's non-cancelled appointments in the requested date range")
        List<PatientAppointmentCalendarResponse> patientAppointments
) {
}
