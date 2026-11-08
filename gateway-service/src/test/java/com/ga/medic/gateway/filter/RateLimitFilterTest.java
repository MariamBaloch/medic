package com.ga.medic.gateway.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void rateLimitsAuthEndpointsExceptPasswordChangeAndLogout() {
        assertThat(RateLimitFilter.isRateLimited("/auth/users/login")).isTrue();
        assertThat(RateLimitFilter.isRateLimited("/auth/users/register/patient")).isTrue();
        assertThat(RateLimitFilter.isRateLimited("/auth/users/forgot-password")).isTrue();
        assertThat(RateLimitFilter.isRateLimited("/auth/users/change-password")).isFalse();
        assertThat(RateLimitFilter.isRateLimited("/auth/users/logout")).isFalse();
    }

    @Test
    void doesNotRateLimitNonAuthEndpoints() {
        assertThat(RateLimitFilter.isRateLimited("/patient/appointments")).isFalse();
        assertThat(RateLimitFilter.isRateLimited("/notifications")).isFalse();
        assertThat(RateLimitFilter.isRateLimited("/doctors/availability/rules")).isFalse();
    }
}
