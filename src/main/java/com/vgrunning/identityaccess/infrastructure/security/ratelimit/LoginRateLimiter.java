package com.vgrunning.identityaccess.infrastructure.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.time.Duration;

/** Limita intentos de acceso en memoria para el único nodo del MVP. */
public final class LoginRateLimiter {
    // ponytail: usar Bucket4j JDBC solo cuando el MVP se despliegue en más de un nodo.
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final Cache<String, Bucket> buckets;
    private final Counter allowedAttempts;
    private final Counter blockedAttempts;

    public LoginRateLimiter(MeterRegistry meterRegistry) {
        buckets =
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterAccess(WINDOW)
                        .recordStats()
                        .build();
        CaffeineCacheMetrics.monitor(meterRegistry, buckets, "login_rate_limit");
        allowedAttempts =
                Counter.builder("identity_access.login_attempts")
                        .tag("result", "allowed")
                        .register(meterRegistry);
        blockedAttempts =
                Counter.builder("identity_access.login_attempts")
                        .tag("result", "blocked")
                        .register(meterRegistry);
    }

    /** Consume ambos límites antes de verificar credenciales para no distinguir resultados. */
    public void checkAndConsume(String email, String remoteAddress) {
        ConsumptionProbe account = bucket("account:" + email, 5).tryConsumeAndReturnRemaining(1);
        ConsumptionProbe ip = bucket("ip:" + remoteAddress, 20).tryConsumeAndReturnRemaining(1);
        if (!account.isConsumed() || !ip.isConsumed()) {
            long waitNanos =
                    Math.max(account.getNanosToWaitForRefill(), ip.getNanosToWaitForRefill());
            blockedAttempts.increment();
            throw new RateLimitedException(Duration.ofNanos(Math.max(1, waitNanos)));
        }
        allowedAttempts.increment();
    }

    private Bucket bucket(String key, long limit) {
        return buckets.get(
                key,
                ignored ->
                        Bucket.builder()
                                .addLimit(
                                        Bandwidth.builder()
                                                .capacity(limit)
                                                .refillIntervally(limit, WINDOW)
                                                .build())
                                .build());
    }
}
