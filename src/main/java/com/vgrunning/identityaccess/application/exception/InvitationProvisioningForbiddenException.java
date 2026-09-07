package com.vgrunning.identityaccess.application.exception;

/** El actor que solicita una invitación no es administrador. */
public final class InvitationProvisioningForbiddenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public String code() {
        return "invitation_provisioning_forbidden";
    }

    @Override
    public String getMessage() {
        return "No tienes permiso para crear invitaciones.";
    }
}
