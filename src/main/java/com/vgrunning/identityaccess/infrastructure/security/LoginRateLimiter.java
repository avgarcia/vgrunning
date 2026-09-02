package com.vgrunning.identityaccess.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;

/** Limita fallos de acceso en memoria para el único nodo del MVP. */
public final class LoginRateLimiter {
    // ponytail: usar Bucket4j JDBC solo cuando el MVP se despliegue en más de un nodo.
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final Cache<String, Bucket> buckets =
            Caffeine.newBuilder().maximumSize(10_000).expireAfterAccess(WINDOW).build();

    public boolean limited(String email, String remoteAddress) {
        return bucket("account:" + email, 5).getAvailableTokens() == 0
                || bucket("ip:" + remoteAddress, 20).getAvailableTokens() == 0;
    }

    public boolean registerFailure(String email, String remoteAddress) {
        boolean accountLimited = !bucket("account:" + email, 5).tryConsume(1);
        boolean ipLimited = !bucket("ip:" + remoteAddress, 20).tryConsume(1);
        return accountLimited || ipLimited;
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
