package com.ga.medic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AppointmentStatusCountFilterRequest {

    @Schema(description = "Earliest appointment date (dd/MM/yyyy)")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateFrom;

    @Schema(description = "Latest appointment date (dd/MM/yyyy)")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateTo;

    @Schema(description = "Doctor's medical specialization ID", example = "1")
    private Long specializationId;
}
