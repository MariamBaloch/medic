package com.ga.medic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Doctor information associated with an appointment")
public record AppointmentDoctorResponse(

        @Schema(description = "Doctor profile ID", example = "1")
        Long doctorId,

        @Schema(description = "Doctor's full name", example = "Dr. Mariam Ali")
        String doctorName,

        @Schema(description = "Doctor's profile image URL")
        String doctorImageUrl,

        @Schema(description = "Doctor's medical specialization details")
        SpecializationResponse specialization,

        @Schema(description = "Doctor's qualification", example = "MBBS, MD")
        String doctorQualification
) {
}
