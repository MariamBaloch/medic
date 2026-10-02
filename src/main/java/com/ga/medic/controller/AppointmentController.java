package com.ga.medic.controller;

import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.service.AppointmentService;
import com.ga.medic.service.AvailabilityRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patient/appointments")
@Tag(name = "Appointments", description = "Patient appointment booking and management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final AvailabilityRuleService availabilityRuleService;

    @GetMapping
    @Operation(summary = "Get my appointments", description = "List appointments for the authenticated patient with pagination and sorting")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PageResponse<AppointmentResponse>> getMyAppointments(@ParameterObject @PageableDefault(sort = {"appointmentDate", "startTime"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(appointmentService.getMyAppointments(pageable));
    }

}