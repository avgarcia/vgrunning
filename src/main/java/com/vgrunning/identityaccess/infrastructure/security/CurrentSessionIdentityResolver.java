package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.application.model.SessionIdentity;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resuelve la identidad de sesión que Spring Security autenticó para la petición actual. */
public final class CurrentSessionIdentityResolver {

    public SessionIdentity current() {
        Authentication authentication =
                Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                        .orElseThrow(AuthenticationRequiredException::new);
        if (authentication.getPrincipal() instanceof SessionIdentity session) {
            return session;
        }
        throw new AuthenticationRequiredException();
    }
}
