package com.ga.medic.controller;

import com.ga.medic.dto.response.AppointmentDetailResponse;
import com.ga.medic.dto.response.AppointmentStatusResponse;
import com.ga.medic.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/appointments")
@Tag(name = "Appointments", description = "Appointment lookup data")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "List appointment statuses",
            description = "Returns all appointment statuses with their display labels, for filtering and display.")
    @GetMapping("/statuses")
    public ResponseEntity<List<AppointmentStatusResponse>> getAppointmentStatuses() {
        return ResponseEntity.ok(appointmentService.getAppointmentStatuses());
    }

    @Operation(summary = "Get appointment details",
            description = "Returns detailed appointment and profile information to the patient or doctor associated with the appointment.")
    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentDetailResponse> getAppointmentById(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(appointmentService.getAppointmentDetails(appointmentId));
    }
}