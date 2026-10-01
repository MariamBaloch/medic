package com.ga.medic.controller;


import com.ga.medic.dto.request.AvailabilityRuleRequest;
import com.ga.medic.dto.response.RuleUpdateResponse;
import com.ga.medic.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/doctors/availability")
@Tag(name = "Doctor Availability")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class DoctorAvailabilityController {

    private final AvailabilityService availabilityService;


    @Operation(summary = "Create availability rule",
            description = "Create a new availability rule defining the doctor's working schedule")
    @PostMapping("/rules")
    public ResponseEntity<RuleUpdateResponse> createRule(@Valid @RequestBody AvailabilityRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.createRule(request));
    }

}