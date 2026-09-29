package com.ga.medic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @Schema(description = "User email", example = "user@example.com")
        String email,
        @Schema(description = "User password", example = "Potato!")
        String password
) {

}