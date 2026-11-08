package com.ga.medic.controller;

import com.ga.medic.annotation.AuditLogger;
import com.ga.medic.enums.AuditAction;
import com.ga.medic.enums.AuditEntityType;
import com.ga.medic.service.ProfileService;
import com.ga.medic.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Administrative user and doctor profile management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final ProfileService profileService;
    private final UserService userService;

    @Operation(summary = "Verify a doctor",
            description = "Marks the doctor profile with the given profile ID as verified.")
    @PatchMapping("/doctors/{doctorProfileId}/verify")
    @AuditLogger(action = AuditAction.UPDATE, entityType = AuditEntityType.PROFILE,
            description = "Admin verified a doctor profile")
    public ResponseEntity<String> verifyDoctor(@PathVariable Long doctorProfileId) {
        profileService.verifyDoctor(doctorProfileId);
        return ResponseEntity.ok("Doctor verified successfully.");
    }

    @Operation(summary = "Soft delete a user",
            description = "Marks the user as deleted while retaining the user and related records.")
    @DeleteMapping("/users/{userId}")
    @AuditLogger(action = AuditAction.DELETE, entityType = AuditEntityType.USER,
            description = "Admin soft deleted a user")
    public ResponseEntity<String> softDeleteUser(@PathVariable Long userId) {
        userService.softDeleteUser(userId);
        return ResponseEntity.ok("User deleted successfully.");
    }
}
