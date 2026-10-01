package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityRuleUpdateResponse(

        @Schema(description = "The created or updated rule")
        AvailabilityRuleResponse rule,

        List<AppointmentResponse> affectedAppointments,

        @Schema(description = "Informational message about affected appointments")
        String message
) {
}