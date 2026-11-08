package com.ga.medic.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ga.medic.config.Constants;
import com.ga.medic.enums.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Editable fields for the authenticated user's account. Email cannot be changed.")
public record UserAccountUpdateRequest(

        @Schema(description = "First name", example = "Mariam", maxLength = 100)
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Schema(description = "Last name", example = "Ali", maxLength = 100)
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Schema(description = "Date of birth, format dd/MM/yyyy", example = "09/09/1999")
        @JsonFormat(pattern = "dd/MM/yyyy")
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Schema(description = "Phone number, digits with an optional leading +", example = "+97312345678")
        @NotBlank(message = "Phone is required")
        @Size(max = 20, message = "Phone must not exceed 20 characters")
        @Pattern(regexp = Constants.PHONE_REGEX,
                message = "Phone must contain only digits, with an optional leading +")
        String phone,

        @Schema(description = "Gender", example = "FEMALE")
        GenderEnum gender
) {
}
