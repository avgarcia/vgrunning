package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.infrastructure.security.session.SessionPrincipal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resuelve la identidad de sesión que Spring Security autenticó para la petición actual. */
public final class CurrentSessionIdentityResolver {

    /** Obtiene el principal técnico para los adaptadores HTTP del propio módulo. */
    public SessionPrincipal currentSession() {
        return findCurrentSession().orElseThrow(AuthenticationRequiredException::new);
    }

    /** Encuentra el principal de sesión cuando la petición ya fue autenticada. */
    public Optional<SessionPrincipal> findCurrentSession() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(SessionPrincipal.class::isInstance)
                .map(SessionPrincipal.class::cast);
    }
}
