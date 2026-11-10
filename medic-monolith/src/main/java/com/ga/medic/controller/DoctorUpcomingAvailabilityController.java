package com.ga.medic.controller;

import com.ga.medic.dto.response.DoctorUpcomingAvailabilityResponse;
import com.ga.medic.service.DoctorUpcomingAvailabilityService;
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
    public ResponseEntity<List<DoctorUpcomingAvailabilityResponse>> getUpcomingAvailability(
            @RequestParam List<Long> ids
    ) {
        return ResponseEntity.ok(doctorUpcomingAvailabilityService.getUpcomingAvailability(ids));
    }
}
