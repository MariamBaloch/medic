package com.ga.medic.common.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Live doctor profile returned by identity for appointment details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalDoctorProfileResponse(
        Long doctorId,
        String doctorName,
        String imageUrl,
        String qualification,
        Long specializationId,
        String specializationName,
        String specializationDescription,
        Integer yearsOfExperience,
        String hospitalAffiliation,
        String clinicAddress,
        String licenseNumber,
        String bio
) {
}
