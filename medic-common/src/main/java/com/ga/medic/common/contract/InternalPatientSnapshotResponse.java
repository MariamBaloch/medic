package com.ga.medic.common.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalPatientSnapshotResponse(
        Long patientId,
        Long patientUserId,
        String patientName,
        String patientEmail
) {
}
