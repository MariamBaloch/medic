package com.ga.medic.controller;

import com.ga.medic.dto.response.UserAccountResponse;
import com.ga.medic.service.DoctorDirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/doctors")
@Tag(name = "Doctors", description = "Doctor directory")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DoctorDirectoryController {

    private final DoctorDirectoryService doctorDirectoryService;

    @Operation(summary = "Retrieve active doctors", description = "Returns active doctors, optionally searched by name/email and filtered by specialization or future availability")
    @GetMapping
    public ResponseEntity<List<UserAccountResponse>> getActiveDoctors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long specializationId,
            @RequestParam(required = false) Boolean hasFutureAvailability
    ) {
        return ResponseEntity.ok(doctorDirectoryService.getActiveDoctors(search, specializationId, hasFutureAvailability));
    }
}
