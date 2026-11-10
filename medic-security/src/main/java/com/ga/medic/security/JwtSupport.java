package com.ga.medic.security;

import com.ga.medic.common.security.JwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtSupport {

    @Value("${jwt.secret-key}")
    private String jwtSecret;

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public UserPrincipal toPrincipal(Claims claims) {
        Number uid = claims.get(JwtClaims.USER_ID, Number.class);
        if (uid == null) {
            throw new IllegalArgumentException("JWT missing uid claim");
        }

        String email = claims.get(JwtClaims.EMAIL, String.class);
        if (email == null || email.isBlank()) {
            email = claims.getSubject();
        }

        String name = claims.get(JwtClaims.NAME, String.class);
        if (name == null || name.isBlank()) {
            name = email;
        }

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get(JwtClaims.ROLES, List.class);
        if (roles == null) {
            roles = List.of();
        }

        Number verNumber = claims.get(JwtClaims.TOKEN_VERSION, Number.class);
        int ver = verNumber == null ? 0 : verNumber.intValue();

        return new UserPrincipal(
                uid.longValue(),
                email,
                name,
                List.copyOf(roles),
                toLong(claims.get(JwtClaims.PATIENT_ID)),
                toLong(claims.get(JwtClaims.DOCTOR_ID)),
                ver
        );
    }

    public UserPrincipal parsePrincipal(String token) {
        return toPrincipal(extractClaims(token));
    }

    public String getJti(String token) {
        return extractClaims(token).getId();
    }

    public Date getExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public boolean validate(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception exception) {
            log.warn("Invalid JWT: {}", exception.getMessage());
            return false;
        }
    }
}
