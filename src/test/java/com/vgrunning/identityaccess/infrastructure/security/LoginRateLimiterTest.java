package com.vgrunning.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    @Test
    void limitsTheSixthFailureForTheSameAccount() {
        LoginRateLimiter limiter = new LoginRateLimiter();

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(limiter.registerFailure("runner@example.invalid", "127.0.0.1")).isFalse();
        }

        assertThat(limiter.registerFailure("runner@example.invalid", "127.0.0.1")).isTrue();
        assertThat(limiter.limited("runner@example.invalid", "127.0.0.1")).isTrue();
    }
}
