package com.ga.medic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Patient information associated with an appointment")
public record AppointmentPatientResponse(

        @Schema(description = "Patient profile ID", example = "2")
        Long patientId,

        @Schema(description = "Patient's full name", example = "Fatima Hassan")
        String patientName,

        @Schema(description = "Patient's profile image URL")
        String patientImageUrl
) {
}
