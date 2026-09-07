package com.vgrunning.identityaccess.infrastructure.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    @Test
    void limitsTheSixthAttemptForTheSameAccount() {
        LoginRateLimiter limiter = new LoginRateLimiter(new SimpleMeterRegistry());

        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.checkAndConsume("runner@example.invalid", "127.0.0.1");
        }

        assertThatThrownBy(() -> limiter.checkAndConsume("runner@example.invalid", "127.0.0.1"))
                .isInstanceOf(RateLimitedException.class);
    }

    @Test
    void consumesTheAccountBucketWhenTheIpBucketRejects() {
        LoginRateLimiter limiter = new LoginRateLimiter(new SimpleMeterRegistry());
        IntStream.range(0, 20)
                .forEach(
                        attempt ->
                                limiter.checkAndConsume(
                                        "other-" + attempt + "@example.invalid", "127.0.0.1"));

        assertThatThrownBy(() -> limiter.checkAndConsume("runner@example.invalid", "127.0.0.1"))
                .isInstanceOf(RateLimitedException.class);

        for (int attempt = 0; attempt < 4; attempt++) {
            limiter.checkAndConsume("runner@example.invalid", "127.0.0." + (attempt + 2));
        }
        assertThatThrownBy(() -> limiter.checkAndConsume("runner@example.invalid", "127.0.0.9"))
                .isInstanceOf(RateLimitedException.class);
    }
}
