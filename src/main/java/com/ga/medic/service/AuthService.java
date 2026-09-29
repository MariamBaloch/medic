package com.ga.medic.service;

import com.ga.medic.dto.UserRegistrationRequest;
import com.ga.medic.enums.RoleEnum;
import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.exception.InformationExistsException;
import com.ga.medic.exception.InformationNotFoundException;
import com.ga.medic.mapper.UserMapper;
import com.ga.medic.model.User;
import com.ga.medic.repository.RoleRepository;
import com.ga.medic.repository.UserRepository;
import com.ga.medic.security.JWTUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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


    public void registerPatient(UserRegistrationRequest request) {
        if (userRepository.existsByEmail((request.email()))) {
            throw new InformationExistsException("User with email address " + request.email() + " already exists");
        } else {
            User user = userMapper.toEntity(request);
            user.setVerificationToken(UUID.randomUUID().toString());
            user.setTokenExpiry(LocalDateTime.now().plusHours(24));

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(user.getEmail());
            msg.setSubject("Verify your email");
            msg.setText("Click to verify: http://localhost:8080/auth/users/verify?token="
                    + user.getVerificationToken());
            mailSender.send(msg);

            user.setRole(roleRepository.getByName(RoleEnum.PATIENT));
            user.setPassword(passwordEncoder.encode(request.password()));
            userRepository.save(user);
        }
    }

    public boolean verify(String token) {
        User user = userRepository.findByVerificationToken(token).orElseThrow(() ->
                new InformationNotFoundException("User with token " + token + " not found")
        );
        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) return false;
        user.setStatus(UserStatusEnum.ACTIVE);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);
        return true;
    }

}
