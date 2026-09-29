package com.ga.medic.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ga.medic.config.Constants;
import com.ga.medic.enums.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserRegistrationRequest {
    @Schema(description = "Email address", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    public String email;

    @Schema(description = "Min 5 characters, with at least one uppercase letter, one lowercase letter and one special character", example = "Potato!")
    @NotBlank(message = "Password is required")
    @Pattern(regexp = Constants.PASSWORD_REGEX,
            message = "Password must be at least 5 characters long and contain at least one uppercase letter, one lowercase letter, and one special character")
    public String password;

    @Schema(description = "Profile picture path")
    public String imageUrl;

    @Schema(description = "First name", example = "Mariam", maxLength = 100)
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    public String firstName;

    @Schema(description = "Last name", example = "Ali", maxLength = 100)
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    public String lastName;

    @Schema(description = "Date of birth, format dd/MM/yyyy", example = "09/09/1999")
    @JsonFormat(pattern = "dd/MM/yyyy")
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    public LocalDate dateOfBirth;

    @Schema(description = "Phone number, digits with an optional leading +", example = "+97312345678")
    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Pattern(regexp = Constants.PHONE_REGEX, message = "Phone must contain only digits, with an optional leading +")
    public String phone;

    @Schema(description = "Gender")
    public GenderEnum gender;
}