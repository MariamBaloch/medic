package com.ga.medic.booking.support;

import com.ga.medic.common.security.JwtClaims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class TestJwtFactory {

    public static final String DEFAULT_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    public static final long DEFAULT_EXPIRATION_MS = 3_600_000L;

    private TestJwtFactory() {
    }

    public static String patientToken(long userId, String email, String name, long patientId, int tokenVersion) {
        return token(email, userId, email, name, List.of("PATIENT"), tokenVersion, patientId, null);
    }

    public static String doctorToken(long userId, String email, String name, long doctorId, int tokenVersion) {
        return token(email, userId, email, name, List.of("DOCTOR"), tokenVersion, null, doctorId);
    }

    public static String token(
            String subject,
            long userId,
            String email,
            String name,
            List<String> roles,
            int tokenVersion,
            Long patientId,
            Long doctorId
    ) {
        return token(subject, userId, email, name, roles, tokenVersion, patientId, doctorId,
                DEFAULT_SECRET, DEFAULT_EXPIRATION_MS);
    }

    public static String token(
            String subject,
            long userId,
            String email,
            String name,
            List<String> roles,
            int tokenVersion,
            Long patientId,
            Long doctorId,
            String secret,
            long expirationMs
    ) {
        var builder = Jwts.builder()
                .setSubject(subject)
                .setId(UUID.randomUUID().toString())
                .claim(JwtClaims.USER_ID, userId)
                .claim(JwtClaims.EMAIL, email)
                .claim(JwtClaims.NAME, name)
                .claim(JwtClaims.ROLES, roles)
                .claim(JwtClaims.TOKEN_VERSION, tokenVersion)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs));

        if (patientId != null) {
            builder.claim(JwtClaims.PATIENT_ID, patientId);
        }
        if (doctorId != null) {
            builder.claim(JwtClaims.DOCTOR_ID, doctorId);
        }

        return builder.signWith(SignatureAlgorithm.HS256, secret).compact();
    }
}
