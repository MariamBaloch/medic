package com.ga.medic.identity.dto.response;

public record InternalPatientSnapshotResponse(
        Long patientId,
        Long patientUserId,
        String patientName,
        String patientEmail
) {
}
