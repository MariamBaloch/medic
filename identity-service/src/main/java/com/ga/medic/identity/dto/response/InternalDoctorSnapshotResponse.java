package com.ga.medic.identity.dto.response;

import java.math.BigDecimal;

public record InternalDoctorSnapshotResponse(
        Long doctorId,
        Long doctorUserId,
        String doctorName,
        String doctorEmail,
        Long doctorSpecializationId,
        BigDecimal consultationFee
) {
}
