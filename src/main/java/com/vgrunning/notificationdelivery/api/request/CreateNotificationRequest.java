package com.vgrunning.notificationdelivery.api.request;

import java.util.Objects;
import java.util.UUID;

/** Sobre cifrado que identidad entrega a notificaciones sin revelar sus valores. */
public record CreateNotificationRequest(
        UUID id,
        String logicalKey,
        UUID originId,
        EncryptedValue destination,
        EncryptedValue payload,
        UUID correlationId) {
    public CreateNotificationRequest {
        Objects.requireNonNull(id);
        Objects.requireNonNull(logicalKey);
        Objects.requireNonNull(originId);
        Objects.requireNonNull(destination);
        Objects.requireNonNull(payload);
        Objects.requireNonNull(correlationId);
    }
}
