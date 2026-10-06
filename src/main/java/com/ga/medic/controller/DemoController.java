package com.ga.medic.controller;

import com.ga.medic.security.MyUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// TODO delete file
@RestController
@RequestMapping("/api/v1/demo")
@Tag(name = "Demo / Security Testing")
@SecurityRequirement(name = "bearerAuth")
public class DemoController {

    @Operation(summary = "Patient Dashboard Test", description = "Accessible only by users with ROLE_PATIENT")
    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Map<String, String>> getPatientData(@AuthenticationPrincipal MyUserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Welcome to the Patient Portal, " + userDetails.user().getFirstName() + "!",
                "role", userDetails.user().getRole().getName().name()
        ));
    }

    @Operation(summary = "Doctor Dashboard Test", description = "Accessible only by users with ROLE_DOCTOR")
    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, String>> getDoctorData(@AuthenticationPrincipal MyUserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Welcome to the Doctor Portal, Dr. " + userDetails.user().getLastName() + "!",
                "role", userDetails.user().getRole().getName().name()
        ));
    }
}