package com.ga.medic.common.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Live patient profile returned by identity for appointment details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalPatientProfileResponse(
        Long patientId,
        String patientName,
        String imageUrl,
        String address,
        String city,
        String country,
        String bloodType,
        Integer heightCm,
        Integer weightKg,
        String allergies,
        String chronicConditions
) {
}
