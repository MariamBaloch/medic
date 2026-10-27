package com.ga.medic.dto.request;

import com.ga.medic.enums.WaitlistStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AppointmentWaitlistFilterRequest {

    @Schema(description = "Earliest requested appointment date (dd/MM/yyyy)")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateFrom;

    @Schema(description = "Latest requested appointment date (dd/MM/yyyy)")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateTo;

    @Schema(description = "Waitlist entry status")
    private WaitlistStatusEnum status;
}
