package com.vgrunning.identityaccess.infrastructure.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades secretas de despliegue necesarias para la infraestructura de acceso. */
@ConfigurationProperties(prefix = "pmv.identity")
public record IdentityAccessProperties(String rateLimitHmacKey, Duration securityEventRetention) {
    public IdentityAccessProperties {
        if (rateLimitHmacKey == null || rateLimitHmacKey.isBlank()) {
            throw new IllegalArgumentException("pmv.identity.rate-limit-hmac-key es obligatoria.");
        }
        if (securityEventRetention == null
                || securityEventRetention.isZero()
                || securityEventRetention.isNegative()) {
            throw new IllegalArgumentException(
                    "pmv.identity.security-event-retention debe ser una duración positiva.");
        }
    }
}
