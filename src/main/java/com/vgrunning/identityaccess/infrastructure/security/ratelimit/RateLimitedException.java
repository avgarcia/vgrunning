package com.vgrunning.identityaccess.infrastructure.security.ratelimit;

import java.time.Duration;

/** Señala que la ventana fija de credenciales ya no admite más intentos. */
public final class RateLimitedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final Duration retryAfter;

    public RateLimitedException(Duration retryAfter) {
        super("No se puede intentar el acceso en este momento");
        this.retryAfter = retryAfter;
    }

    public String code() {
        return "rate_limit_exceeded";
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
