package com.ga.medic.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Blocks public access to service-to-service paths at the gateway.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class BlockInternalPathFilter extends OncePerRequestFilter {

    public static boolean isInternalPath(String path) {
        return path.equals("/internal") || path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (isInternalPath(request.getRequestURI())) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
