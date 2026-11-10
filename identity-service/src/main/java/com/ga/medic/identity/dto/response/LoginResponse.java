package com.ga.medic.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "JWT Token")
        String token
) {
}