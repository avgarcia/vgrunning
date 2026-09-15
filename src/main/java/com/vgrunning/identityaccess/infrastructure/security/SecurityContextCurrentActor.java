package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import com.vgrunning.identityaccess.api.actor.CurrentActor;
import com.vgrunning.identityaccess.infrastructure.security.session.SessionPrincipal;
import lombok.RequiredArgsConstructor;

/**
 * Resuelve el actor autenticado directamente sobre el {@code SecurityContext} vigente.
 *
 * <p>Lee {@code SecurityContextHolder} a través de {@link CurrentSessionIdentityResolver}, así que
 * no puede vivir en {@code application}: es un contrato de {@code api} servido desde {@code
 * infrastructure}, la misma tensión señalada en H5 de la auditoría de estructura de paquetería para
 * {@code notificationdelivery}. No hay otro alojamiento legal: la firma del controlador que lo
 * consume está generada desde OpenAPI y no admite un parámetro nuevo.
 */
@RequiredArgsConstructor
public final class SecurityContextCurrentActor implements CurrentActor {
    private final CurrentSessionIdentityResolver sessions;

    @Override
    public ActorContext require() {
        SessionPrincipal principal = sessions.currentSession();
        return new ActorContext(principal.accountId(), principal.role().value());
    }
}
