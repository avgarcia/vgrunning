package com.vgrunning.identityaccess.application.model;

/** Resultado de autenticar credenciales; Spring Session crea la sesión HTTP. */
public record SessionLogin(SessionIdentity session) {
    public static SessionLogin create(SessionIdentity session) {
        return new SessionLogin(session);
    }
}
