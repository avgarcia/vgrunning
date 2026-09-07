package com.vgrunning.identityaccess.application.exception;

/** El correo solicitado ya tiene una reserva vigente en identidad. */
public final class EmailAlreadyReservedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public String code() {
        return "email_already_reserved";
    }

    @Override
    public String getMessage() {
        return "El correo ya está reservado.";
    }
}
