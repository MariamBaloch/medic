package com.ga.medic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DoctorProfileRequest(
        @Schema(description = "Medical specialization ID", example = "1")
        Long specializationId,

        @Schema(description = "Doctor's qualification", example = "MBBS, MD", maxLength = 255)
        @Size(max = 255, message = "Qualification must not exceed 255 characters")
        String qualification,

        @Schema(description = "Years of professional experience", example = "10", minimum = "0")
        @PositiveOrZero(message = "Years of experience must not be negative")
        Integer yearsOfExperience,

        @Schema(description = "Consultation fee", example = "25.00", minimum = "0")
        @DecimalMin(value = "0.0", message = "Consultation fee must not be negative")
        BigDecimal consultationFee,

        @Schema(description = "Hospital affiliation", example = "Manama Medical Center", maxLength = 255)
        @Size(max = 255, message = "Hospital affiliation must not exceed 255 characters")
        String hospitalAffiliation,

        @Schema(description = "Clinic address", example = "Building 45, Road 3621, Manama")
        String clinicAddress,

        @Schema(description = "Professional biography", example = "Dr. Mariam is a highly experienced cardiologist with over 20 years of practice.")
        String bio
) {
}
