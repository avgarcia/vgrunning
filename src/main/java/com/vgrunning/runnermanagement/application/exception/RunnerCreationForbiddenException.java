package com.vgrunning.runnermanagement.application.exception;

/** El actor autenticado no puede crear corredores. */
public final class RunnerCreationForbiddenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public String code() {
        return "runner_creation_forbidden";
    }

    @Override
    public String getMessage() {
        return "No tienes permiso para crear corredores.";
    }
}
