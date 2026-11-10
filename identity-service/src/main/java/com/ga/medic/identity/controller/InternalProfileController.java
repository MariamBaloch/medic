package com.ga.medic.identity.controller;

import com.ga.medic.identity.dto.response.InternalDoctorSnapshotResponse;
import com.ga.medic.identity.dto.response.InternalPatientSnapshotResponse;
import com.ga.medic.identity.service.InternalProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalProfileController {

    private final InternalProfileService internalProfileService;

    @GetMapping("/patients/{id}")
    public ResponseEntity<InternalPatientSnapshotResponse> getPatient(@PathVariable Long id) {
        return ResponseEntity.ok(internalProfileService.getPatientSnapshot(id));
    }

    @GetMapping("/doctors/{id}")
    public ResponseEntity<InternalDoctorSnapshotResponse> getDoctor(@PathVariable Long id) {
        return ResponseEntity.ok(internalProfileService.getDoctorSnapshot(id));
    }
}
