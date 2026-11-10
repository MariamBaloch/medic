package com.ga.medic.dto.response;

public record DoctorUpcomingAvailabilityResponse(
        Long doctorId,
        boolean hasUpcomingAvailability
) {
}
