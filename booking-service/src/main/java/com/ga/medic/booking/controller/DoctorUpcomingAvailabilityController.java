package com.ga.medic.booking.controller;

import com.ga.medic.booking.dto.response.DoctorUpcomingAvailabilityResponse;
import com.ga.medic.booking.service.DoctorUpcomingAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/doctors/availability")
@Tag(name = "Doctor Upcoming Availability", description = "Whether listed doctors have upcoming availability")
public class DoctorUpcomingAvailabilityController {

    private final DoctorUpcomingAvailabilityService doctorUpcomingAvailabilityService;

    @GetMapping("/upcoming")
    @Operation(summary = "Check upcoming availability for doctors",
            description = "For each doctor id, reports whether that doctor has a current or future availability rule. Used to compose the directory without loading every slot.")
    public ResponseEntity<List<DoctorUpcomingAvailabilityResponse>> getUpcomingAvailability(
            @Parameter(description = "Doctor profile ids to check", required = true)
            @RequestParam List<Long> ids
    ) {
        return ResponseEntity.ok(doctorUpcomingAvailabilityService.getUpcomingAvailability(ids));
    }
}
