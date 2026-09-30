package com.ga.medic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record DoctorProfileResponse(
        @Schema(description = "Medical specialization ID", example = "1")
        Long specializationId,

        @Schema(description = "Doctor's qualification", example = "MBBS, MD")
        String qualification,

        @Schema(description = "Years of professional experience", example = "10")
        Integer yearsOfExperience,

        @Schema(description = "Consultation fee", example = "25.00")
        BigDecimal consultationFee,

        @Schema(description = "Hospital affiliation", example = "Manama Medical Center")
        String hospitalAffiliation,

        @Schema(description = "Clinic address", example = "Building 45, Road 3621, Manama")
        String clinicAddress,

        @Schema(description = "Professional biography")
        String bio,

        AuditResponse audit
) {
}
