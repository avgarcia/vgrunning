package com.vgrunning.identityaccess.application;

import java.time.Duration;

/** Señala que la ventana fija de credenciales ya no admite más intentos. */
public final class RateLimitedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final Duration retryAfter;

    public RateLimitedException(Duration retryAfter) {
        super("Se alcanzó el límite temporal de intentos.");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
