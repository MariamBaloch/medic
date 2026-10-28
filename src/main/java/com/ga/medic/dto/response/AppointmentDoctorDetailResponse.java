package com.ga.medic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Doctor profile information associated with the appointment")
public record AppointmentDoctorDetailResponse(

        @Schema(description = "Doctor profile ID", example = "1")
        Long doctorId,

        @Schema(description = "Doctor's full name", example = "Dr. Mariam Ali")
        String doctorName,

        @Schema(description = "Doctor's profile image URL")
        String doctorImageUrl,

        @Schema(description = "Doctor's qualification", example = "MBBS, MD")
        String doctorQualification,

        @Schema(description = "Doctor's medical specialization details")
        SpecializationResponse specialization,

        @Schema(description = "Years of professional experience", example = "10")
        Integer doctorYearsOfExperience,

        @Schema(description = "Hospital affiliation", example = "Manama Medical Center")
        String doctorHospitalAffiliation,

        @Schema(description = "Clinic address", example = "Building 45, Road 3621, Manama")
        String doctorClinicAddress,

        @Schema(description = "Medical license number", example = "MD10001")
        String doctorLicenseNumber,

        @Schema(description = "Professional biography")
        String doctorBio
) {
}
