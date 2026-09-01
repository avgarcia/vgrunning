package com.vgrunning.identityaccess.domain.securityevent;

import java.time.Duration;
import java.time.OffsetDateTime;

/** Vigencia explícita usada para calcular la retención de un evento de seguridad. */
public record SecurityEventRetention(Duration duration) {

    public SecurityEventRetention {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    "La retención de eventos debe ser una duración positiva.");
        }
    }

    public OffsetDateTime retentionUntil(OffsetDateTime occurredAt) {
        return occurredAt.plus(duration);
    }
}
