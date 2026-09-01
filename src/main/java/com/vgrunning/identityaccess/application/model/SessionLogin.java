package com.vgrunning.identityaccess.application.model;

/** Resultado efímero de iniciar sesión; el secreto solo se conserva para emitir la cookie. */
public record SessionLogin(SessionIdentity session, String rawSessionToken) {
    public static SessionLogin create(SessionIdentity session, String rawSessionToken) {
        return new SessionLogin(session, rawSessionToken);
    }
}
