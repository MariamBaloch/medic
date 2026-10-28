package com.ga.medic.controller;

import com.ga.medic.dto.response.SpecializationResponse;
import com.ga.medic.service.SpecializationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/specializations")
@Tag(name = "Specializations", description = "Specialization lookup")
@RequiredArgsConstructor
public class SpecializationController {

    private final SpecializationService specializationService;

    @Operation(summary = "Retrieve specializations", description = "Returns all specializations, optionally filtered by name or description")
    @GetMapping
    public ResponseEntity<List<SpecializationResponse>> getSpecializations(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(specializationService.getSpecializations(search));
    }
}
