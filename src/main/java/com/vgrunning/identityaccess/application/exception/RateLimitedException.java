package com.vgrunning.identityaccess.application.exception;

import java.time.Duration;

/** Señala que la ventana fija de credenciales ya no admite más intentos. */
public final class RateLimitedException extends IdentityAccessException {
    private static final long serialVersionUID = 1L;
    private final Duration retryAfter;

    public RateLimitedException(Duration retryAfter) {
        super("rate_limit_exceeded", "No se puede intentar el acceso en este momento");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
