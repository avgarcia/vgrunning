package com.vgrunning.runnermanagement.application.exception;

/** La misma clave no puede representar dos solicitudes lógicas distintas. */
public final class IdempotencyKeyReusedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public String code() {
        return "idempotency_key_reused";
    }

    @Override
    public String getMessage() {
        return "La clave de idempotencia ya se ha usado con otra solicitud.";
    }
}
