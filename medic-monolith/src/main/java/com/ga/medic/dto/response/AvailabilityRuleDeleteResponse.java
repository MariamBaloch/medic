package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityRuleDeleteResponse(

        @Schema(description = "Whether the availability rule was permanently deleted. False means the rule was soft-deleted by setting its end date to yesterday.", example = "true")
        boolean hardDeleted,

        @Schema(description = "Appointments that were detached from the availability rule before it was permanently deleted.")
        List<AppointmentResponse> affectedAppointments,

        @Schema(description = "Additional information about the deletion and any affected appointments.", example = "2 existing appointment(s) were detached from the deleted rule")
        String message
) {
}