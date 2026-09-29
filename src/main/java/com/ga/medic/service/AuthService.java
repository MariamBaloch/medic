package com.ga.medic.service;

import com.ga.medic.dto.request.DoctorRegistrationRequest;
import com.ga.medic.dto.request.LoginRequest;
import com.ga.medic.dto.request.UserRegistrationRequest;
import com.ga.medic.dto.response.LoginResponse;
import com.ga.medic.dto.response.UserRegistrationResponse;
import com.ga.medic.enums.RoleEnum;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.exception.InformationExistsException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.mapper.UserMapper;
import com.ga.medic.model.*;
import com.ga.medic.repository.RoleRepository;
import com.ga.medic.repository.SpecializationRepository;
import com.ga.medic.repository.UserRepository;
import com.ga.medic.security.JWTUtils;
import com.ga.medic.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Transactional
    public UserRegistrationResponse registerPatient(UserRegistrationRequest request) {
        User user = createUser(request, RoleEnum.PATIENT);

        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        user.setPatientProfile(profile);

        User newUser = userRepository.save(user);
        return userMapper.toResponse(newUser);
    }

    @Transactional
    public UserRegistrationResponse registerDoctor(DoctorRegistrationRequest request) {
        User user = createUser(request, RoleEnum.DOCTOR);

        Specialization specialization = specializationRepository
                .findById(request.getSpecializationId())
                .orElseThrow(() ->
                        new InformationNotFoundException("Specialization with id " + request.getSpecializationId() + " not found"));

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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        MyUserDetails myUserDetails = (MyUserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateJwtToken(myUserDetails);

        return new LoginResponse(jwtToken);
    }
}