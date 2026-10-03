package com.ga.medic.controller;

import com.ga.medic.dto.request.AppointmentFilterRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/doctors/appointments")
@Tag(name = "Doctor > Appointments", description = "Doctor appointment history")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class DoctorAppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Get my appointment history",
            description = "Returns the authenticated doctor's appointments, including past appointments, with optional patient, date-range, and status filters.")
    public ResponseEntity<PageResponse<AppointmentResponse>> getMyAppointments(
            @Parameter(description = "Patient profile ID") @RequestParam(required = false) Long patientId,
            @ParameterObject AppointmentFilterRequest filters,
            @ParameterObject @PageableDefault(sort = {"appointmentDate", "startTime"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(appointmentService.getDoctorAppointments(patientId, filters, pageable));
    }
}
