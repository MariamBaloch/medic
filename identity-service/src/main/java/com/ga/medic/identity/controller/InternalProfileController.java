package com.ga.medic.identity.controller;

import com.ga.medic.common.contract.InternalDoctorProfileResponse;
import com.ga.medic.common.contract.InternalDoctorSnapshotResponse;
import com.ga.medic.common.contract.InternalPatientProfileResponse;
import com.ga.medic.common.contract.InternalPatientSnapshotResponse;
import com.ga.medic.common.contract.InternalProfileImagesResponse;
import com.ga.medic.identity.service.InternalProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Tag(name = "Internal profiles", description = "Service-to-service profile reads. Requires the X-Internal-Key header.")
@SecurityRequirement(name = "internalApiKey")
public class InternalProfileController {

    private final InternalProfileService internalProfileService;

    @GetMapping("/profile-images")
    @Operation(summary = "Get profile photo URLs",
            description = "Returns the current public photo path for the requested doctor and patient profile ids.")
    public ResponseEntity<InternalProfileImagesResponse> getProfileImages(
            @Parameter(description = "Doctor profile ids") @RequestParam(required = false) List<Long> doctorIds,
            @Parameter(description = "Patient profile ids") @RequestParam(required = false) List<Long> patientIds) {
        return ResponseEntity.ok(internalProfileService.getProfileImages(doctorIds, patientIds));
    }

    @GetMapping("/patients/{id}")
    @Operation(summary = "Get a patient snapshot",
            description = "Returns the patient fields stored on a booking or waitlist row: id, user id, name, and email.")
    public ResponseEntity<InternalPatientSnapshotResponse> getPatient(
            @Parameter(description = "Patient profile id") @PathVariable Long id) {
        return ResponseEntity.ok(internalProfileService.getPatientSnapshot(id));
    }

    @GetMapping("/patients/{id}/profile")
    @Operation(summary = "Get a live patient profile",
            description = "Returns the current patient profile, including details that are not copied onto a booking.")
    public ResponseEntity<InternalPatientProfileResponse> getPatientProfile(
            @Parameter(description = "Patient profile id") @PathVariable Long id) {
        return ResponseEntity.ok(internalProfileService.getPatientProfile(id));
    }

    @GetMapping("/doctors/{id}")
    @Operation(summary = "Get a doctor snapshot",
            description = "Returns the doctor fields stored on a booking or waitlist row, including specialization name and consultation fee.")
    public ResponseEntity<InternalDoctorSnapshotResponse> getDoctor(
            @Parameter(description = "Doctor profile id") @PathVariable Long id) {
        return ResponseEntity.ok(internalProfileService.getDoctorSnapshot(id));
    }

    @GetMapping("/doctors/{id}/profile")
    @Operation(summary = "Get a live doctor profile",
            description = "Returns the current doctor profile, including photo, qualification, and biography.")
    public ResponseEntity<InternalDoctorProfileResponse> getDoctorProfile(
            @Parameter(description = "Doctor profile id") @PathVariable Long id) {
        return ResponseEntity.ok(internalProfileService.getDoctorProfile(id));
    }
}
