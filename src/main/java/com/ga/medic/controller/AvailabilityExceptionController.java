package com.ga.medic.controller;

import com.ga.medic.dto.request.AvailabilityExceptionRequest;
import com.ga.medic.dto.response.AvailabilityExceptionDeleteResponse;
import com.ga.medic.dto.response.AvailabilityExceptionResponse;
import com.ga.medic.service.AvailabilityExceptionService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/doctors/availability/exceptions")
@Tag(name = "Doctor Availability")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class AvailabilityExceptionController {

    private final AvailabilityExceptionService availabilityExceptionService;

    @Operation(summary = "Create availability exception",
            description = "Block a whole day or a time window. Existing appointments are kept but counted in the response.")
    @PostMapping
    public ResponseEntity<AvailabilityExceptionResponse> createException(@Valid @RequestBody AvailabilityExceptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityExceptionService.createException(request));
    }

    @GetMapping
    @Operation(summary = "List availability exceptions",
            description = "Returns all current and future availability exceptions for the authenticated doctor.")
    public ResponseEntity<List<AvailabilityExceptionResponse>> listExceptions() {
        return ResponseEntity.ok(availabilityExceptionService.listExceptions());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update availability exception",
            description = "Updates an existing availability exception and returns any existing appointments affected by the new exception.")
    public ResponseEntity<AvailabilityExceptionResponse> updateException(@PathVariable Long id, @Valid @RequestBody AvailabilityExceptionRequest request) {
        return ResponseEntity.ok(availabilityExceptionService.updateException(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete availability exception",
            description = "Deletes an availability exception and returns any existing appointments affected by the deletion.")
    public ResponseEntity<AvailabilityExceptionDeleteResponse> deleteException(@PathVariable Long id) {
        return ResponseEntity.ok(availabilityExceptionService.deleteException(id));
    }
}