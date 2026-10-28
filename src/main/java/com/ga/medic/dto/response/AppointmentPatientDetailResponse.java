package com.ga.medic.dto.response;

import com.ga.medic.enums.BloodTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Patient identity and profile information associated with the appointment")
public record AppointmentPatientDetailResponse(

        @Schema(description = "Patient profile ID", example = "2")
        Long patientId,

        @Schema(description = "Patient's full name", example = "Fatima Hassan")
        String patientName,

        @Schema(description = "Patient's profile image URL")
        String patientImageUrl,

        @Schema(description = "Patient address")
        String address,

        @Schema(description = "Patient city")
        String city,

        @Schema(description = "Patient country")
        String country,

        @Schema(description = "Patient blood type", example = "A+")
        BloodTypeEnum bloodType,

        @Schema(description = "Patient height in centimeters", example = "170")
        Integer heightCm,

        @Schema(description = "Patient weight in kilograms", example = "60")
        Integer weightKg,

        @Schema(description = "Known allergies")
        String allergies,

        @Schema(description = "Chronic conditions")
        String chronicConditions
) {
}
