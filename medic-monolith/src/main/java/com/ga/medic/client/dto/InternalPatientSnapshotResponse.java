package com.ga.medic.client.dto;

public record InternalPatientSnapshotResponse(
        Long patientId,
        Long patientUserId,
        String patientName,
        String patientEmail
) {
}
