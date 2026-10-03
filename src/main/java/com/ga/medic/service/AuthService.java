package com.ga.medic.service;

import com.ga.medic.dto.request.*;
import com.ga.medic.dto.response.LoginResponse;
import com.ga.medic.dto.response.UserAccountResponse;
import com.ga.medic.enums.RoleEnum;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.exception.InformationExistsException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.mapper.UserMapper;
import com.ga.medic.model.*;
import com.ga.medic.repository.DoctorProfileRepository;
import com.ga.medic.repository.RoleRepository;
import com.ga.medic.repository.SpecializationRepository;
import com.ga.medic.repository.UserRepository;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.security.JWTUtils;
import com.ga.medic.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    @Lazy
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    @Lazy
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final JavaMailSender mailSender;
    private final SpecializationRepository specializationRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AuthenticatedUser authenticatedUser;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public UserAccountResponse registerPatient(UserRegistrationRequest request) {
        User user = createUser(request, RoleEnum.PATIENT);

        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        user.setPatientProfile(profile);

        User newUser = userRepository.save(user);
        return userMapper.toResponse(newUser);
    }

    @Transactional
    public UserAccountResponse registerDoctor(DoctorRegistrationRequest request) {
        User user = createUser(request, RoleEnum.DOCTOR);

        Specialization specialization = specializationRepository
                .findById(request.getSpecializationId())
                .orElseThrow(() ->
                        new InformationNotFoundException("Specialization with id " + request.getSpecializationId() + " not found"));

        if (doctorProfileRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new InformationExistsException("License number is already registered");
        }
        DoctorProfile profile = new DoctorProfile();
        profile.setUser(user);
        profile.setSpecialization(specialization);
        profile.setLicenseNumber(request.getLicenseNumber());

        user.setDoctorProfile(profile);

        User newUser = userRepository.save(user);

        return userMapper.toResponse(newUser);
    }

    private User createUser(UserRegistrationRequest request, RoleEnum roleEnum) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InformationExistsException(
                    "User with email address " + request.getEmail() + " already exists");
        }

        User user = userMapper.toUser(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = roleRepository.findByName(roleEnum)
                .orElseThrow(() ->
                        new InformationNotFoundException(roleEnum + " role not found"));

        user.setRole(role);
        user.setStatus(UserStatusEnum.PENDING_VERIFICATION);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setTokenExpiry(LocalDateTime.now().plusHours(24));
//      TODO enable later, disabled to prevent spam
//        sendVerificationEmail(user);
        return user;
    }

    @Transactional
    public boolean verify(String token) {
        User user = userRepository.findByVerificationToken(token).orElseThrow(() ->
                new InformationNotFoundException("Invalid verification token"));

        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) return false;

        user.setStatus(UserStatusEnum.ACTIVE);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);
        return true;
    }

    private void sendVerificationEmail(User user) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(user.getEmail());
        msg.setSubject("Verify your email");
        msg.setText("Click to verify: http://localhost:8080/auth/users/verify?token="
                + user.getVerificationToken());
        mailSender.send(msg);
    }

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new BadCredentialsException("Password is incorrect");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        MyUserDetails myUserDetails = (MyUserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateJwtToken(myUserDetails);

        return new LoginResponse(jwtToken);
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InformationNotFoundException("User with email " + email + " not found"));

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.token())
                .orElseThrow(() -> new InformationNotFoundException("Invalid or expired password reset token"));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        return true;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = authenticatedUser.get().user();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private void sendPasswordResetEmail(String email, String token) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Password Reset Token");
        msg.setText("Your password reset token is:\n\n"
                + token + "\n\n"
                + "Use this token to reset your password by sending a POST request to /auth/users/reset-password with your token and new password.\n\n"
                + "This token will expire in 15 minutes.");
        mailSender.send(msg);
    }

    public void logout(String authHeader) {
        String JwtToken = authHeader.substring(7);
        String jti = jwtUtils.getJtiFromJwtToken(JwtToken);
        Date expiresAt = jwtUtils.getExpirationFromJwtToken(JwtToken);

        tokenBlacklistService.blacklist(jti, expiresAt);
    }
}