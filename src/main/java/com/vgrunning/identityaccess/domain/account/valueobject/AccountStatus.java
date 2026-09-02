package com.vgrunning.identityaccess.domain.account.valueobject;

import java.util.Arrays;

/** Estados persistidos del ciclo de vida de una cuenta. */
public enum AccountStatus {
    PENDING_ACTIVATION("pending_activation"),
    ACTIVE("active"),
    DISABLED("disabled"),
    PENDING_REACTIVATION("pending_reactivation"),
    CANCELLED("cancelled");

    private final String value;

    AccountStatus(String value) {
        this.value = value;
    }

    /** Devuelve el valor estable almacenado en PostgreSQL y publicado por la API. */
    public String value() {
        return value;
    }

    /** Resuelve el estado a partir de su valor estable almacenado. */
    public static AccountStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equals(value))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Estado de cuenta desconocido: " + value));
    }
}
