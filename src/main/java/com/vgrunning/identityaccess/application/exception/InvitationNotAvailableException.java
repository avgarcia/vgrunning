package com.vgrunning.identityaccess.application.exception;

/** Respuesta pública indistinguible para un desafío inexistente, usado o caducado. */
public final class InvitationNotAvailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public String code() {
        return "invitation_not_available";
    }

    @Override
    public String getMessage() {
        return "La invitación no está disponible.";
    }
}
