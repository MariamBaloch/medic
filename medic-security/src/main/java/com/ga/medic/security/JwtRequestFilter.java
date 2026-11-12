package com.ga.medic.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private TokenVersionService tokenVersionService;
    @Autowired
    private JwtSupport jwtSupport;

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String jwt = parseJwt(request);
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtSupport.extractClaims(jwt);
            String jti = claims.getId();
            if (tokenBlacklistService.isBlacklisted(jti)) {
                UnauthorizedResponseWriter.write(request, response, "Token has been revoked");
                return;
            }

            UserPrincipal principal = jwtSupport.toPrincipal(claims);
            if (!tokenVersionService.matches(principal.userId(), principal.tokenVersion())) {
                UnauthorizedResponseWriter.write(request, response, "Token version is no longer valid");
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            log.warn("Expired JWT on {}", request.getRequestURI());
            UnauthorizedResponseWriter.write(request, response, "JWT token has expired");
        } catch (JwtException exception) {
            log.warn("Invalid JWT on {}: {}", request.getRequestURI(), exception.getMessage());
            UnauthorizedResponseWriter.write(request, response, "Invalid JWT token");
        } catch (Exception exception) {
            log.error("Cannot set user authentication on {}", request.getRequestURI(), exception);
            UnauthorizedResponseWriter.write(request, response, "Authentication failed");
        }
    }
}
