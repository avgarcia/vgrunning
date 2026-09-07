package com.vgrunning.identityaccess.api.actor;

import java.util.UUID;

/** Actor autenticado que los adaptadores entregan explícitamente a los casos de uso protegidos. */
public record ActorContext(UUID accountId, String role) {
    public boolean isAdministrator() {
        return "administrador".equals(role);
    }
}
