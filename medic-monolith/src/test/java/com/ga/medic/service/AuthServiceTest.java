package com.ga.medic.service;

import com.ga.medic.dto.request.LoginRequest;
import com.ga.medic.dto.request.UserRegistrationRequest;
import com.ga.medic.enums.RoleEnum;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.exception.EmailVerificationRequiredException;
import com.ga.medic.mapper.UserMapper;
import com.ga.medic.model.Role;
import com.ga.medic.model.User;
import com.ga.medic.repository.DoctorProfileRepository;
import com.ga.medic.repository.RoleRepository;
import com.ga.medic.repository.SpecializationRepository;
import com.ga.medic.repository.UserRepository;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.security.JWTUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JWTUtils jwtUtils;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SpecializationRepository specializationRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private AuthenticatedUser authenticatedUser;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    @Test(expected = BadCredentialsException.class)
    public void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@email.com")).thenReturn(Optional.empty());
        authService.login(new LoginRequest("unknown@email.com", "pass"));
    }

    @Test(expected = BadCredentialsException.class)
    public void login_shouldThrow_whenPasswordIncorrect() {
        User user = new User();
        user.setPassword("encoded");
        user.setStatus(UserStatusEnum.ACTIVE);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded")).thenReturn(false);

        authService.login(new LoginRequest("user@email.com", "wrongpassword"));
    }

    @Test(expected = EmailVerificationRequiredException.class)
    public void login_shouldThrow_whenEmailNotVerified() {
        User user = new User();
        user.setPassword("encoded");
        user.setStatus(UserStatusEnum.PENDING_VERIFICATION);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);

        authService.login(new LoginRequest("user@email.com", "pass"));
    }

    @Test
    public void registerPatient_shouldSave_whenValid() {
        UserRegistrationRequest request = mock(UserRegistrationRequest.class);
        when(request.getEmail()).thenReturn("new@email.com");

        User user = new User();
        Role role = new Role();

        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(userMapper.toUser(any())).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(roleRepository.findByName(RoleEnum.PATIENT)).thenReturn(Optional.of(role));
        when(userRepository.save(any())).thenReturn(user);

        authService.registerPatient(request);

        verify(userRepository).save(any(User.class));
    }
}
