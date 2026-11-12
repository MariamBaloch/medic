package com.ga.medic.booking.dto.response;

public record DoctorUpcomingAvailabilityResponse(
        Long doctorId,
        boolean hasUpcomingAvailability
) {
}
