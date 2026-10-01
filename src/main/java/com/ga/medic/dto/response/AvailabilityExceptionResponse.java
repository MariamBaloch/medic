package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Availability exception details")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityExceptionResponse(

        @Schema(description = "Exception ID", example = "1")
        Long id,

        @Schema(description = "Date of the exception", example = "15/10/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate exceptionDate,

        @Schema(description = "Start time of exception window. Null for a whole-day exception.", example = "09:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @Schema(description = "End time of exception window. Null for a whole-day exception.", example = "12:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        @Schema(description = "Reason for the exception", example = "Doctor attending a conference")
        String reason,

        @Schema(description = "Audit information")
        AuditResponse audit,

        @Schema(description = "Existing appointments affected by this exception but kept", implementation = AppointmentResponse.class)
        List<AppointmentResponse> affectedAppointments,

        @Schema(description = "Message describing the effect of the exception", example = "2 existing appointment(s) are affected by this exception but were kept")
        String message
) {
}