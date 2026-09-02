package com.vgrunning.identityaccess.domain.account.valueobject;

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

    public String value() {
        return value;
    }

    public static AccountStatus fromValue(String value) {
        for (AccountStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de cuenta desconocido: " + value);
    }
}
