package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.infrastructure.security.session.SessionPrincipal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resuelve la identidad de sesión que Spring Security autenticó para la petición actual. */
public final class CurrentSessionIdentityResolver {

    /**
     * Obtiene el principal propio de la sesión actual o rechaza una autenticación no compatible.
     */
    public SessionPrincipal current() {
        Authentication authentication =
                Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                        .orElseThrow(AuthenticationRequiredException::new);
        if (authentication.getPrincipal() instanceof SessionPrincipal session) {
            return session;
        }
        throw new AuthenticationRequiredException();
    }
}
