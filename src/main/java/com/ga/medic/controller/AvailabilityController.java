package com.ga.medic.controller;


import com.ga.medic.dto.request.AvailabilityRuleRequest;
import com.ga.medic.dto.response.AvailabilityRuleResponse;
import com.ga.medic.dto.response.AvailabilityRuleUpdateResponse;
import com.ga.medic.service.AvailabilityRuleService;
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
@RequestMapping("/doctors/availability")
@Tag(name = "Doctor Availability")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityRuleService availabilityRuleService;

    @Operation(summary = "List availability rules",
            description = "List current and future availability rules")
    @GetMapping("/rules")
    public ResponseEntity<List<AvailabilityRuleResponse>> listRules() {
        return ResponseEntity.ok(availabilityRuleService.listRules());
    }

    @Operation(summary = "Create availability rule",
            description = "Create a new availability rule defining the doctor's working schedule")
    @PostMapping("/rules")
    public ResponseEntity<AvailabilityRuleUpdateResponse> createRule(@Valid @RequestBody AvailabilityRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityRuleService.createRule(request));
    }

    @Operation(summary = "Update availability rule",
            description = "Update an existing availability rule. If the rule has already started, the old rule is closed and a new one is created.")
    @PutMapping("/rules/{id}")
    public ResponseEntity<AvailabilityRuleUpdateResponse> updateRule(@PathVariable Long id, @Valid @RequestBody AvailabilityRuleRequest request) {
        return ResponseEntity.ok(availabilityRuleService.updateRule(id, request));
    }


}