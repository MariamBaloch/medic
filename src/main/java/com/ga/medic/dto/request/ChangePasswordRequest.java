package com.ga.medic.dto.request;

import com.ga.medic.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @Schema(description = "Current password", example = "Potato!")
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(description = "Min 5 characters, with at least one uppercase letter, one lowercase letter and one special character", example = "Potato!")
        @NotBlank(message = "Password is required")
        @Pattern(regexp = Constants.PASSWORD_REGEX,
                message = "Password must be at least 5 characters long and contain at least one uppercase letter, one lowercase letter, and one special character")
        String newPassword
) {
}