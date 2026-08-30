package com.vgrunning.identityaccess.application;

/** Respuesta deliberadamente indistinguible para cualquier fallo de credenciales. */
public final class InvalidCredentialsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("Las credenciales no permiten iniciar sesión.");
    }
}
