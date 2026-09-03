package com.vgrunning.identityaccess.infrastructure.security;

/** Ausencia o invalidez de la sesión opaca actual. */
public final class AuthenticationRequiredException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AuthenticationRequiredException() {
        super("Es necesario iniciar sesión");
    }

    public String code() {
        return "authentication_required";
    }
}
