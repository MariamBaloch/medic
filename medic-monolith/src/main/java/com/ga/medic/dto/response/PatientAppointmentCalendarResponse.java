package com.ga.medic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public record PatientAppointmentCalendarResponse(

        Long appointmentId,

        String doctorName,

        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate date,

        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        String reason
) {
}