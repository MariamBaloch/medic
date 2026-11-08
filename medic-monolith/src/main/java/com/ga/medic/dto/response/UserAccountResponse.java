package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ga.medic.enums.GenderEnum;
import com.ga.medic.enums.RoleEnum;
import com.ga.medic.enums.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserAccountResponse(
        @Schema(example = "1")
        Long userId,

        @Schema(example = "user@example.com")
        String email,

        @Schema(example = "Mariam")
        String firstName,

        @Schema(example = "Ali")
        String lastName,

        @Schema(example = "09/09/1999")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate dateOfBirth,

        @Schema(example = "+97312345678")
        String phone,

        GenderEnum gender,
        @Schema(description = "Account role", example = "PATIENT")
        RoleEnum role,

        @Schema(description = "Account status", example = "PENDING_VERIFICATION")
        UserStatusEnum status,

        @Schema(description = "Profile image URL")
        String imageUrl,

        @Schema(description = "Present only when role is PATIENT")
        PatientProfileResponse patient,

        @Schema(description = "Present only when role is DOCTOR")
        DoctorProfileResponse doctor,

        AuditResponse audit
) {
}

