package com.ga.medic.service;

import com.ga.medic.dto.request.*;
import com.ga.medic.dto.response.LoginResponse;
import com.ga.medic.dto.response.UserAccountResponse;
import com.ga.medic.enums.RoleEnum;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.exception.EmailVerificationRequiredException;
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

import static com.ga.medic.config.Constants.EMAIL_VERIFICATION_TOKEN_EXPIRY_MINUTES;
import static com.ga.medic.config.Constants.PASSWORD_RESET_TOKEN_EXPIRY_MINUTES;

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

    /**
     * Registers a patient account and creates its patient profile.
     *
     * @param request the registration details
     * @return the created user account details
     */
    @Transactional
    public UserAccountResponse registerPatient(UserRegistrationRequest request) {
        User user = createUser(request, RoleEnum.PATIENT, true);

        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        user.setPatientProfile(profile);

        User newUser = userRepository.save(user);
        return userMapper.toResponse(newUser);
    }

    /**
     * Registers a doctor with a specialization and a license number that is not already in use.
     *
     * @param request the doctor registration details
     * @return the created user account details
     */
    @Transactional
    public UserAccountResponse registerDoctor(DoctorRegistrationRequest request) {
        User user = createUser(request, RoleEnum.DOCTOR, true);

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

    /**
     * Registers an active administrator account without requiring email verification.
     *
     * @param request the administrator registration details
     * @return the created user account details
     */
    @Transactional
    public UserAccountResponse registerAdmin(UserRegistrationRequest request) {
        User user = createUser(request, RoleEnum.ADMIN, false);
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Builds a user with an encoded password and applies the role-specific activation and verification state.
     *
     * @param request the registration details used to build the account
     * @param roleEnum the role assigned to the account
     * @param requiresEmailVerification whether the account must verify its email before activation
     * @return the initialized user entity
     */
    private User createUser(UserRegistrationRequest request, RoleEnum roleEnum, boolean requiresEmailVerification) {
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
        if (requiresEmailVerification) {
            user.setStatus(UserStatusEnum.PENDING_VERIFICATION);
            generateVerificationToken(user);
//          TODO enable later, disabled to prevent spam
            sendVerificationEmail(user);
        } else {
            user.setStatus(UserStatusEnum.ACTIVE);
        }
        return user;
    }

    /**
     * Activates the account when its verification token is valid; returns false when the token has expired.
     *
     * @param token the email verification token
     * @return true if the account was activated, or false if the token has expired
     */
    @Transactional
    public boolean verify(String token) {
        User user = userRepository.findByVerificationToken(token).orElseThrow(() ->
                new InformationNotFoundException("Invalid verification token"));

        if (user.getTokenExpiry() == null || !user.getTokenExpiry().isAfter(LocalDateTime.now())) return false;

        user.setStatus(UserStatusEnum.ACTIVE);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);
        return true;
    }

    /**
     * Sends the account's verification token and expiry information to its email address.
     *
     * @param user the account whose email address and verification token are used
     */
    private void sendVerificationEmail(User user) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(user.getEmail());
        msg.setSubject("Verify your email");
        msg.setText("Click to verify: http://localhost:8080/auth/users/verify?token=" + user.getVerificationToken()
                + " \n\nThe token expires in " + EMAIL_VERIFICATION_TOKEN_EXPIRY_MINUTES + " minutes.");
        mailSender.send(msg);
    }

    /**
     * Assigns a random verification token and its configured expiry to the account.
     *
     * @param user the account receiving the token
     */
    private void generateVerificationToken(User user) {
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(EMAIL_VERIFICATION_TOKEN_EXPIRY_MINUTES));
    }

    /**
     * Authenticates the user and issues a JWT, sending verification when a pending account has no valid token.
     *
     * @param loginRequest the user's email and password
     * @return the issued JWT
     */
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getStatus() == UserStatusEnum.DELETED || user.getDeletedAt() != null) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new BadCredentialsException("Password is incorrect");
        }

        if (user.getStatus() == UserStatusEnum.PENDING_VERIFICATION) {
            if (user.getVerificationToken() != null && user.getTokenExpiry() != null && user.getTokenExpiry().isAfter(LocalDateTime.now())) {
                throw new EmailVerificationRequiredException("Email verification is required. Please use the verification email already sent.");
            }

            generateVerificationToken(user);
            userRepository.save(user);
            sendVerificationEmail(user);
            throw new EmailVerificationRequiredException("Email verification is required. A new verification email has been sent. The token expires in "
                    + EMAIL_VERIFICATION_TOKEN_EXPIRY_MINUTES + " minutes.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        MyUserDetails myUserDetails = (MyUserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateJwtToken(myUserDetails);

        return new LoginResponse(jwtToken);
    }

    /**
     * Creates a time-limited password reset token and sends it to the user's email address.
     *
     * @param email the email address of the account requesting a password reset
     */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InformationNotFoundException("User with email " + email + " not found"));

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_EXPIRY_MINUTES));

        userRepository.save(user);

        sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    /**
     * Resets the password with a valid token, returning false if the token has expired.
     *
     * @param request the reset token and new password
     * @return true if the password was reset, or false if the token has expired
     */
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

    /**
     * Changes the authenticated user's password after verifying the current password.
     *
     * @param request the current and new passwords
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = authenticatedUser.get().user();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Sends a password reset token and its expiry information to the specified email address.
     *
     * @param email the destination email address
     * @param token the password reset token
     */
    private void sendPasswordResetEmail(String email, String token) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Password Reset Token");
        msg.setText("Your password reset token is:\n\n"
                + token + "\n\n"
                + "Use this token to reset your password by sending a POST request to /auth/users/reset-password with your token and new password.\n\n"
                + "This token will expire in " + PASSWORD_RESET_TOKEN_EXPIRY_MINUTES + " minutes.");
        mailSender.send(msg);
    }

    /**
     * Revokes the current JWT by blacklisting it until its expiration time.
     *
     * @param authHeader the authorization header containing the JWT
     */
    public void logout(String authHeader) {
        String JwtToken = authHeader.substring(7);
        String jti = jwtUtils.getJtiFromJwtToken(JwtToken);
        Date expiresAt = jwtUtils.getExpirationFromJwtToken(JwtToken);

        tokenBlacklistService.blacklist(jti, expiresAt);
    }
}
