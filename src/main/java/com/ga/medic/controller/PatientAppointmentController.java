package com.ga.medic.controller;

import com.ga.medic.annotation.AuditLogger;
import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.request.AppointmentFilterRequest;
import com.ga.medic.dto.request.CancelAppointmentRequest;
import com.ga.medic.dto.request.RescheduleAppointmentRequest;
import com.ga.medic.dto.response.AppointmentDetailResponse;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AvailableSlotResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.enums.AuditAction;
import com.ga.medic.enums.AuditEntityType;
import com.ga.medic.service.AppointmentService;
import com.ga.medic.service.AvailabilityRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/patient/appointments")
@Tag(name = "Patient > Appointments", description = "Patient appointment booking and management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
public class PatientAppointmentController {
    private final AppointmentService appointmentService;
    private final AvailabilityRuleService availabilityRuleService;

    @GetMapping
    @Operation(summary = "Get my appointments",
            description = "Lists the authenticated patient's appointments with optional doctor, date-range, status, and specialization filters.")
    public ResponseEntity<PageResponse<AppointmentResponse>> getMyAppointments(
            @Parameter(description = "Doctor profile ID") @RequestParam(required = false) Long doctorId,
            @ParameterObject AppointmentFilterRequest filters,
            @ParameterObject @PageableDefault(sort = {"appointmentDate", "startTime"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(appointmentService.getPatientAppointments(doctorId, filters, pageable));
    }

    @GetMapping("/{appointmentId}")
    @Operation(summary = "Get appointment details",
            description = "Returns the details of one of the authenticated patient's appointments, including the doctor's profile information.")
    public ResponseEntity<AppointmentDetailResponse> getAppointmentById(
            @Parameter(description = "Appointment ID") @PathVariable Long appointmentId) {
        return ResponseEntity.ok(appointmentService.getPatientAppointment(appointmentId));
    }

    @PostMapping
    @Operation(summary = "Book an appointment",
            description = "Book an appointment with a doctor. The system validates the slot is truly available against current rules and prevents double-booking.")
    @AuditLogger(action = AuditAction.BOOK, entityType = AuditEntityType.APPOINTMENT, description = "Patient booked an appointment")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody AppointmentBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookAppointment(request));
    }

    @PutMapping("/{appointmentId}/reschedule")
    @Operation(summary = "Reschedule an appointment",
            description = "Moves the authenticated patient's future booked appointment to another currently available slot with the same doctor. The request must include the appointment version last read by the patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment rescheduled"),
            @ApiResponse(responseCode = "400", description = "Invalid request or appointment cannot be rescheduled"),
            @ApiResponse(responseCode = "403", description = "Appointment belongs to another patient"),
            @ApiResponse(responseCode = "404", description = "Appointment or patient profile not found"),
            @ApiResponse(responseCode = "409", description = "Version is stale or target slot is unavailable")
    })
    @AuditLogger(action = AuditAction.UPDATE, entityType = AuditEntityType.APPOINTMENT,
            description = "Patient rescheduled an appointment")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @Parameter(description = "Appointment ID") @PathVariable Long appointmentId,
            @Valid @RequestBody RescheduleAppointmentRequest request
    ) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(appointmentId, request));
    }

    @Operation(summary = "Cancel an appointment",
            description = "Cancel an appointment. The freed slot reappears automatically if the current rules still produce it.")
    @PutMapping("/{id}/cancel")
    @AuditLogger(action = AuditAction.CANCEL, entityType = AuditEntityType.APPOINTMENT, description = "Patient cancelled an appointment")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long id, @Valid @RequestBody CancelAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, request));
    }

    @Operation(summary = "Get doctor availability",
            description = "View available appointment slots for a specific doctor within a date range.")
    @GetMapping("/doctors/{doctorId}/availability")
    public ResponseEntity<List<AvailableSlotResponse>> getDoctorAvailability(
            @PathVariable Long doctorId,
            @Parameter(description = "Start date (dd/MM/yyyy)", required = true)
            @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate from,
            @Parameter(description = "End date (dd/MM/yyyy)", required = true)
            @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate to) {
        return ResponseEntity.ok(availabilityRuleService.getAvailability(doctorId, from, to));
    }
}
