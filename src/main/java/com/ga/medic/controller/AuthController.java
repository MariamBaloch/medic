package com.ga.medic.controller;

import com.ga.medic.dto.request.*;
import com.ga.medic.dto.response.LoginResponse;
import com.ga.medic.dto.response.UserAccountResponse;
import com.ga.medic.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<UserAccountResponse> registerPatient(@Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerPatient(request));
    }

    @Operation(summary = "Register a doctor",
            description = "Creates a doctor account in PENDING_VERIFICATION status and emails a verification link (valid 24 hours).")
    @PostMapping("/register/doctor")
    public ResponseEntity<UserAccountResponse> registerDoctor(@Valid @RequestBody DoctorRegistrationRequest request) {
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

    @Operation(summary = "Forgot Password",
            description = "Sends a password reset token link to the user's registered email address.")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok("Password reset email sent successfully.");
    }

    @Operation(summary = "Reset Password",
            description = "Resets the user password using a valid reset token.")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean isReset = authService.resetPassword(request);
        return isReset
                ? ResponseEntity.ok("Password has been reset successfully. Please login with your new password.")
                : ResponseEntity.badRequest().body("Token is invalid or has expired.");
    }

    @Operation(summary = "Change Password",
            description = "Allows an authenticated user to change their password by providing the current password.")
    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest passwordRequest, HttpServletRequest request) {
        authService.changePassword(passwordRequest);
        authService.logout(request.getHeader(HttpHeaders.AUTHORIZATION));
        return ResponseEntity.ok("Password updated successfully. Please login again with new credentials.");
    }

    @Operation(summary = "User Logout",
            description = "Invalidates the current JWT token so it can no longer be used for authentication.")
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        authService.logout(request.getHeader(HttpHeaders.AUTHORIZATION));
        return ResponseEntity.ok("Logged out successfully.");
    }
}