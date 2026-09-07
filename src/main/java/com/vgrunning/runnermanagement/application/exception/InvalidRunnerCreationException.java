package com.vgrunning.runnermanagement.application.exception;

/** La representación no conserva los invariantes mínimos del perfil. */
public final class InvalidRunnerCreationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public String code() {
        return "invalid_request";
    }

    @Override
    public String getMessage() {
        return "La solicitud no es válida.";
    }
}
