package com.vgrunning.identityaccess.domain.account.valueobject;

/** Roles persistidos que puede asumir una cuenta de acceso. */
public enum AccountRole {
    ADMINISTRADOR("administrador"),
    ENTRENADOR("entrenador"),
    CORREDOR("corredor");

    private final String value;

    AccountRole(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AccountRole fromValue(String value) {
        for (AccountRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol de cuenta desconocido: " + value);
    }
}
