package com.vgrunning.identityaccess.application;

/** Respuesta deliberadamente indistinguible para cualquier fallo de credenciales. */
public final class InvalidCredentialsException extends IdentityAccessException {
    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("session_creation_rejected", "No se ha podido iniciar sesión");
    }
}
