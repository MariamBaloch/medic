package com.ga.medic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRegistrationRequest(
        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Enter a valid email")
        String email,

        @NotBlank(message = "Password cannot be empty")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()\\-+_=<>?]).{5,}$",
                message = "Password must be at least 5 characters long and contain at least one uppercase letter, one lowercase letter, and one special character")
        String password,

        String imageUrl
) {
}
