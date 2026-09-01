package com.vgrunning.identityaccess.domain.session;

import java.time.Duration;

/** Política aprobada de vigencia y límites de autenticación. */
public record SessionSecurityPolicy(
        Duration idleTimeout,
        Duration absoluteTimeout,
        Duration rateWindow,
        int accountFailureLimit,
        int ipFailureLimit) {

    public SessionSecurityPolicy {
        requirePositive(idleTimeout, "idleTimeout");
        requirePositive(absoluteTimeout, "absoluteTimeout");
        requirePositive(rateWindow, "rateWindow");
        if (accountFailureLimit <= 0 || ipFailureLimit <= 0) {
            throw new IllegalArgumentException("Los límites de intentos deben ser positivos.");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " debe ser una duración positiva.");
        }
    }
}
