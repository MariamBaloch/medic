package com.ga.medic.dto.request;


import com.ga.medic.enums.BloodTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PatientProfileRequest(
        @Schema(description = "Address", example = "Building 45, Road 3621, Block 436, Adliya")
        @Size(max = 100, message = "Address must not exceed 100 characters")
        String address,

        @Schema(description = "City", example = "Manama")
        @Size(max = 35, message = "City must not exceed 35 characters")
        String city,

        @Schema(description = "Country", example = "Bahrain")
        @Size(max = 50, message = "Country must not exceed 50 characters")
        String country,

        @Schema(description = "Blood Type", example = "A+")
        BloodTypeEnum bloodType,

        @Schema(description = "Height in cm", example = "170")
        @Positive(message = "Height must be greater than 0")
        Integer heightCm,

        @Schema(description = "Weight in kg", example = "60")
        @Positive(message = "Weight must be greater than 0")
        Integer weightKg,

        @Schema(description = "Allergies", example = "Peanuts,Cats")
        String allergies,

        @Schema(description = "Chronic Conditions", example = "Hypertension,Diabetes")
        String chronicConditions
) {
}
