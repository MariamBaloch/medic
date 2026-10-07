package com.ga.medic.controller;

import com.ga.medic.annotation.AuditLogger;
import com.ga.medic.dto.request.AppointmentFilterRequest;
import com.ga.medic.dto.request.UpdateAppointmentStatusRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.enums.AuditAction;
import com.ga.medic.enums.AuditEntityType;
import com.ga.medic.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/{appointmentId}/status")
    @Operation(summary = "Update appointment outcome",
            description = "Marks one of the authenticated doctor's booked appointments as COMPLETED or NO_SHOW.")
    @AuditLogger(action = AuditAction.UPDATE, entityType = AuditEntityType.APPOINTMENT,
            description = "Doctor updated an appointment outcome")
    public ResponseEntity<AppointmentResponse> updateAppointmentStatus(@PathVariable Long appointmentId, @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        return ResponseEntity.ok(appointmentService.updateDoctorAppointmentStatus(appointmentId, request.status()));
    }
}