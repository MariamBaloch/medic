package com.ga.medic.dto.response;

import com.ga.medic.enums.BloodTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;

public record PatientProfileResponse(
        @Schema(description = "Address", example = "Building 45, Road 3621, Block 436, Adliya")
        String address,

        @Schema(description = "City", example = "Manama")
        String city,

        @Schema(description = "Country", example = "Bahrain")
        String country,

        @Schema(description = "Blood Type", example = "A+")
        BloodTypeEnum bloodType,

        @Schema(description = "Height in cm", example = "170")
        Integer heightCm,

        @Schema(description = "Weight in kg", example = "60")
        Integer weightKg,

        @Schema(description = "Allergies", example = "Peanuts,Cats")
        String allergies,

        @Schema(description = "Chronic Conditions", example = "Hypertension,Diabetes")
        String chronicConditions,

        AuditResponse audit
) {
}
