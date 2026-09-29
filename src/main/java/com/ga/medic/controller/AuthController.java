package com.ga.medic.controller;

import com.ga.medic.dto.UserRegistrationRequest;
import com.ga.medic.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/patient")
    public ResponseEntity<String> registerPatient(@Valid @RequestBody UserRegistrationRequest request) {
        authService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully registered, please check you email for verification.");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token) {
        return authService.verify(token)
                ? ResponseEntity.ok("Email successfully verified, please login.")
                : ResponseEntity.badRequest().body("Invalid or expired link.");
    }
}
