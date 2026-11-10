package com.ga.medic.identity.service;

import com.ga.medic.identity.dto.request.LoginRequest;
import com.ga.medic.identity.enums.UserStatusEnum;
import com.ga.medic.identity.mapper.UserMapper;
import com.ga.medic.identity.model.User;
import com.ga.medic.identity.repository.DoctorProfileRepository;
import com.ga.medic.identity.repository.RoleRepository;
import com.ga.medic.identity.repository.SpecializationRepository;
import com.ga.medic.identity.repository.UserRepository;
import com.ga.medic.security.AuthenticatedUser;
import com.ga.medic.security.TokenBlacklistService;
import com.ga.medic.security.TokenVersionService;
import com.ga.medic.identity.security.JWTUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JWTUtils jwtUtils;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserMapper userMapper;
    @Mock private RoleRepository roleRepository;
    @Mock private EmailService emailService;
    @Mock private SpecializationRepository specializationRepository;
    @Mock private DoctorProfileRepository doctorProfileRepository;
    @Mock private AuthenticatedUser authenticatedUser;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private TokenVersionService tokenVersionService;
    @Mock private IdentityEventService identityEventService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginRejectsDeletedUsers() {
        User user = new User();
        user.setEmail("deleted@medic.com");
        user.setPassword("hash");
        user.setStatus(UserStatusEnum.DELETED);
        when(userRepository.findByEmail("deleted@medic.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("deleted@medic.com", "secret")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
