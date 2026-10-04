package com.ga.medic.security;

import com.ga.medic.config.RateLimiterConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final RateLimiterConfig rateLimiterConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = getClientIp(request);

        Optional<RateLimitRule> rule = resolveRule(path);

        if (rule.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] key = (ip + "|" + rule.get().group()).getBytes(StandardCharsets.UTF_8);
        Bucket bucket = proxyManager.builder().build(key, rule.get().config);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("""
                    {"error": "TOO_MANY_REQUESTS", "message": "Rate limit exceeded. Please try again after one minute."}
                    """);
        }
    }

    /**
     * Returns the client's IP address for use as the rate-limiting key.
     * Normalizes IPv6 localhost addresses to 127.0.0.1 for consistency.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private Optional<RateLimitRule> resolveRule(String path) {
        if (path.startsWith("/auth/users")
                && !path.equals("/auth/users/change-password")
                && !path.equals("/auth/users/logout")) {
            return Optional.of(new RateLimitRule("auth", rateLimiterConfig.authConfig()));
        }
        return Optional.empty();
    }

    private record RateLimitRule(String group, BucketConfiguration config) {
    }
}