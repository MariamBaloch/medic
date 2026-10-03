package com.ga.medic.controller;

import com.ga.medic.dto.request.AppointmentBookingRequest;
import com.ga.medic.dto.request.AppointmentFilterRequest;
import com.ga.medic.dto.request.CancelAppointmentRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AvailableSlotResponse;
import com.ga.medic.dto.response.PageResponse;
import com.ga.medic.service.AppointmentService;
import com.ga.medic.service.AvailabilityRuleService;
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
            description = "Lists the authenticated patient's appointments with optional doctor, date-range, and status filters.")
    public ResponseEntity<PageResponse<AppointmentResponse>> getMyAppointments(
            @Parameter(description = "Doctor profile ID") @RequestParam(required = false) Long doctorId,
            @ParameterObject AppointmentFilterRequest filters,
            @ParameterObject @PageableDefault(sort = {"appointmentDate", "startTime"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(appointmentService.getPatientAppointments(doctorId, filters, pageable));
    }

    @PostMapping
    @Operation(summary = "Book an appointment",
            description = "Book an appointment with a doctor. The system validates the slot is truly available against current rules and prevents double-booking.")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody AppointmentBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookAppointment(request));
    }

    @Operation(summary = "Cancel an appointment",
            description = "Cancel an appointment. The freed slot reappears automatically if the current rules still produce it.")
    @PutMapping("/appointments/{id}/cancel")
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
