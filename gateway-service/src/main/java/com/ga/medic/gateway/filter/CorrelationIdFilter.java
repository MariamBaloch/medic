package com.ga.medic.gateway.filter;

import com.ga.medic.common.api.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = CorrelationId.resolve(request.getHeader(CorrelationId.HEADER));
        response.setHeader(CorrelationId.HEADER, correlationId);

        try {
            MDC.put(CorrelationId.MDC_KEY, correlationId);
            if (log.isDebugEnabled()) {
                log.debug("Gateway request {} {}", request.getMethod(), request.getRequestURI());
            }
            filterChain.doFilter(withCorrelationId(request, correlationId), response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    private HttpServletRequest withCorrelationId(HttpServletRequest request, String correlationId) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (CorrelationId.HEADER.equalsIgnoreCase(name)) {
                    return correlationId;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (CorrelationId.HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(correlationId));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
                boolean present = names.stream().anyMatch(CorrelationId.HEADER::equalsIgnoreCase);
                if (!present) {
                    names.add(CorrelationId.HEADER);
                }
                return Collections.enumeration(names);
            }
        };
    }
}
