package com.ga.medic.controller;

import com.ga.medic.dto.request.AppointmentWaitlistRequest;
import com.ga.medic.dto.response.AppointmentResponse;
import com.ga.medic.dto.response.AppointmentWaitlistEntryResponse;
import com.ga.medic.service.AppointmentWaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patient/waitlist")
@Tag(name = "Patient > Waitlist", description = "Join waitlists for fully booked appointment slots and manage slot offers")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientWaitlistController {

    private final AppointmentWaitlistService waitlistService;

    @PostMapping
    @Operation(summary = "Join a doctor's waitlist for one exact slot",
            description = "Creates a WAITING entry only when the requested future slot is valid and currently booked or reserved by another offer. An available slot must be booked normally.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Waitlist entry created"),
            @ApiResponse(responseCode = "400", description = "Invalid date, time, or request fields"),
            @ApiResponse(responseCode = "403", description = "Patient role required"),
            @ApiResponse(responseCode = "404", description = "Doctor not found"),
            @ApiResponse(responseCode = "409", description = "Slot is invalid, available to book, overlapping, or already on this patient's active waitlist")
    })
    public ResponseEntity<AppointmentWaitlistEntryResponse> join(@Valid @RequestBody AppointmentWaitlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waitlistService.join(request));
    }

    @GetMapping
    @Operation(summary = "List my waitlist entries",
            description = "Returns only entries owned by the authenticated patient, including offer status and expiration time.")
    @ApiResponse(responseCode = "200", description = "Patient's waitlist entries")
    public ResponseEntity<List<AppointmentWaitlistEntryResponse>> listMine() {
        return ResponseEntity.ok(waitlistService.getMyEntries());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Withdraw or decline a waitlist entry",
            description = "Cancels a WAITING entry. Declining an OFFERED entry immediately advances the slot to the next eligible patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Entry withdrawn or offer declined"),
            @ApiResponse(responseCode = "403", description = "Entry belongs to another patient"),
            @ApiResponse(responseCode = "404", description = "Entry not found"),
            @ApiResponse(responseCode = "409", description = "Entry is no longer active")
    })
    public ResponseEntity<Void> withdraw(@PathVariable Long id) {
        waitlistService.withdraw(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept a waitlist offer",
            description = "Creates the appointment and marks the entry BOOKED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer accepted and appointment created"),
            @ApiResponse(responseCode = "403", description = "Entry belongs to another patient"),
            @ApiResponse(responseCode = "404", description = "Entry not found"),
            @ApiResponse(responseCode = "409", description = "Offer expired, is no longer active, or cannot be accepted")
    })
    public ResponseEntity<AppointmentResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(waitlistService.accept(id));
    }
}
