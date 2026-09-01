package com.vgrunning.identityaccess.infrastructure.security;

/** Fallo técnico de origen o CSRF que la configuración MVC representa como Problem Details. */
public final class CsrfValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CsrfValidationException() {
        super("La solicitud no ha superado la validación de seguridad");
    }

    public String code() {
        return "csrf_validation_failed";
    }
}
