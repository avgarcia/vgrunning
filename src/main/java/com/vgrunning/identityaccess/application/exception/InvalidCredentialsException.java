package com.vgrunning.identityaccess.application.exception;

/** Respuesta deliberadamente indistinguible para cualquier fallo de credenciales. */
public final class InvalidCredentialsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("No se ha podido iniciar sesión");
    }

    public String code() {
        return "session_creation_rejected";
    }
}
