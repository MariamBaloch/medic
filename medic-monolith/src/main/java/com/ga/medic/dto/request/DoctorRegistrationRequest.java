package com.ga.medic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorRegistrationRequest extends UserRegistrationRequest {

    @Schema(description = "Doctor's medical specialization", example = "1")
    @NotNull(message = "Specialization is required")
    private Long specializationId;

    @Schema(description = "Medical license number", example = "BHP-12345", maxLength = 100)
    @NotBlank(message = "License number is required")
    @Size(max = 100, message = "License number must not exceed 100 characters")
    private String licenseNumber;
}