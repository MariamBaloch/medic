package com.ga.medic.common.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalDoctorSnapshotResponse(
        Long doctorId,
        Long doctorUserId,
        String doctorName,
        String doctorEmail,
        Long doctorSpecializationId,
        BigDecimal consultationFee
) {
}
