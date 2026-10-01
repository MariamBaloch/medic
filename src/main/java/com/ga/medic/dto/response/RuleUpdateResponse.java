package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleUpdateResponse(

        @Schema(description = "The created or updated rule")
        AvailabilityRuleResponse rule,

        @Schema(description = "Number of future appointments that fall outside the new schedule")
        Integer affectedAppointments,

        @Schema(description = "Informational message about affected appointments")
        String message
) {
}