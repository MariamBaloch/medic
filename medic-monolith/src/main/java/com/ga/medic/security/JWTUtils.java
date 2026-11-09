package com.ga.medic.security;

import com.ga.medic.common.security.JwtClaims;
import com.ga.medic.common.security.JwtSupport;
import com.ga.medic.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JWTUtils {

    private final JwtSupport jwtSupport;

    @Value("${jwt.secret-key}")
    private String jwtSecret;

    @Value("${jwt.expiration-time}")
    private long jwtExpirationTime;

    public String generateJwtToken(User user) {
        var builder = Jwts.builder()
                .setSubject(user.getEmail())
                .setId(UUID.randomUUID().toString())
                .claim(JwtClaims.USER_ID, user.getId())
                .claim(JwtClaims.EMAIL, user.getEmail())
                .claim(JwtClaims.NAME, user.getFullName())
                .claim(JwtClaims.ROLES, List.of(user.getRole().getName()))
                .claim(JwtClaims.TOKEN_VERSION, user.getTokenVersion())
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtExpirationTime));

        if (user.getPatientProfile() != null) {
            builder.claim(JwtClaims.PATIENT_ID, user.getPatientProfile().getId());
        }
        if (user.getDoctorProfile() != null) {
            builder.claim(JwtClaims.DOCTOR_ID, user.getDoctorProfile().getId());
        }

        return builder.signWith(SignatureAlgorithm.HS256, jwtSecret).compact();
    }

    /**
     * @deprecated prefer {@link #generateJwtToken(User)}
     */
    @Deprecated
    public String generateJwtToken(MyUserDetails myUserDetails) {
        return generateJwtToken(myUserDetails.user());
    }

    public String getJtiFromJwtToken(String token) {
        return jwtSupport.getJti(token);
    }

    public Date getExpirationFromJwtToken(String token) {
        return jwtSupport.getExpiration(token);
    }

    public boolean validateJwtToken(String authToken) {
        return jwtSupport.validate(authToken);
    }
}
