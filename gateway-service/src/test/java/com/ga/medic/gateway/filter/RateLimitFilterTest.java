package com.ga.medic.gateway.filter;

import com.ga.medic.gateway.config.RateLimiterConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiterConfig rateLimiterConfig;

    @Test
    void rateLimitsAuthEndpointsExceptPasswordChangeAndLogout() {
        assertThat(RateLimitFilter.isRateLimited("/api/identity/auth/users/login")).isTrue();
        assertThat(RateLimitFilter.isRateLimited("/api/identity/auth/users/register/patient")).isTrue();
        assertThat(RateLimitFilter.isRateLimited("/api/identity/auth/users/forgot-password")).isTrue();
        assertThat(RateLimitFilter.isRateLimited("/api/identity/auth/users/change-password")).isFalse();
        assertThat(RateLimitFilter.isRateLimited("/api/identity/auth/users/logout")).isFalse();
    }

    @Test
    void doesNotRateLimitNonAuthEndpoints() {
        assertThat(RateLimitFilter.isRateLimited("/api/booking/patient/appointments")).isFalse();
        assertThat(RateLimitFilter.isRateLimited("/api/notification")).isFalse();
        assertThat(RateLimitFilter.isRateLimited("/api/booking/doctors/availability/rules")).isFalse();
        assertThat(RateLimitFilter.isRateLimited("/auth/users/login")).isFalse();
    }

    @Test
    void returns429WhenBucketIsExhausted() throws Exception {
        LettuceBasedProxyManager<byte[]> proxyManager = mock(LettuceBasedProxyManager.class, RETURNS_DEEP_STUBS);
        BucketProxy bucket = mock(BucketProxy.class);
        when(bucket.tryConsume(1)).thenReturn(false);
        when(proxyManager.builder().build(any(byte[].class), any(BucketConfiguration.class))).thenReturn(bucket);
        when(rateLimiterConfig.authConfig()).thenReturn(BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofMinutes(1))
                        .build())
                .build());

        RateLimitFilter filter = new RateLimitFilter(proxyManager, rateLimiterConfig);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/identity/auth/users/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("TOO_MANY_REQUESTS");
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        verify(chain, never()).doFilter(any(), any());
    }
}
