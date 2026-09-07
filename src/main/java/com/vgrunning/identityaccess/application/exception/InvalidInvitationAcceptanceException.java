package com.vgrunning.identityaccess.application.exception;

/** La contraseña presentada no cumple las reglas públicas de activación. */
public final class InvalidInvitationAcceptanceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public String code() {
        return "invalid_request";
    }

    @Override
    public String getMessage() {
        return "La solicitud no es válida.";
    }
}
