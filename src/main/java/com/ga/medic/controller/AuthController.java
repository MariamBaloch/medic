package com.ga.medic.controller;

import com.ga.medic.dto.request.DoctorRegistrationRequest;
import com.ga.medic.dto.request.LoginRequest;
import com.ga.medic.dto.request.UserRegistrationRequest;
import com.ga.medic.dto.response.LoginResponse;
import com.ga.medic.dto.response.UserRegistrationResponse;
import com.ga.medic.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Registration, email verification and login")
@RestController
@RequestMapping("/auth/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a patient",
            description = "Creates a patient account in PENDING_VERIFICATION status and emails a verification link (valid 24 hours).")
    @PostMapping("/register/patient")
    public ResponseEntity<UserRegistrationResponse> registerPatient(@Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerPatient(request));
    }

    @Operation(summary = "Register a doctor",
            description = "Creates a doctor account in PENDING_VERIFICATION status and emails a verification link (valid 24 hours).")
    @PostMapping("/register/doctor")
    public ResponseEntity<UserRegistrationResponse> registerDoctor(@Valid @RequestBody DoctorRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerDoctor(request));
    }

    @Operation(summary = "Verify email",
            description = "Activates the account using the token from the verification email.")
    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token) {
        return authService.verify(token)
                ? ResponseEntity.ok("Email successfully verified, please login.")
                : ResponseEntity.badRequest().body("Invalid or expired link.");
    }

    @Operation(summary = "User Login",
            description = "Authenticates user credentials (email and password) and returns a Bearer JWT token upon successful authentication.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
}