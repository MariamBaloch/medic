package com.ga.medic.controller;

import com.ga.medic.dto.request.DoctorProfileRequest;
import com.ga.medic.dto.request.PatientProfileRequest;
import com.ga.medic.dto.response.DoctorProfileResponse;
import com.ga.medic.dto.response.PatientProfileResponse;
import com.ga.medic.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Patient and doctor profile management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Update Patient Profile",
            description = "Updates patient profile fields if provided")
    @PatchMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientProfileResponse> updatePatientProfile(@RequestBody PatientProfileRequest request) {
        return ResponseEntity.ok().body(profileService.updatePatientProfile(request));
    }

    @Operation(summary = "Update Doctor Profile",
            description = "Updates doctor profile fields if provided")
    @PatchMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorProfileResponse> updateDoctorProfile(@RequestBody DoctorProfileRequest request) {
        return ResponseEntity.ok().body(profileService.updateDoctorProfile(request));
    }
}