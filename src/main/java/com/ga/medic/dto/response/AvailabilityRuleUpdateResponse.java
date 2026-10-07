package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityRuleUpdateResponse(

        @Schema(description = "The resulting rule. When an already-started rule is replaced, this is the newly created rule; use its id for future update or delete requests.")
        AvailabilityRuleResponse rule,

        List<AppointmentResponse> affectedAppointments,

        @Schema(description = "Informational message about affected appointments")
        String message
) {
}
