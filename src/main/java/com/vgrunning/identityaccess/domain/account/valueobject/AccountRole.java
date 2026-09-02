package com.vgrunning.identityaccess.domain.account.valueobject;

import java.util.Arrays;

/** Roles persistidos que puede asumir una cuenta de acceso. */
public enum AccountRole {
    ADMINISTRADOR("administrador"),
    ENTRENADOR("entrenador"),
    CORREDOR("corredor");

    private final String value;

    AccountRole(String value) {
        this.value = value;
    }

    /** Devuelve el valor estable almacenado en PostgreSQL y publicado por la API. */
    public String value() {
        return value;
    }

    /** Resuelve el rol a partir de su valor estable almacenado. */
    public static AccountRole fromValue(String value) {
        return Arrays.stream(values())
                .filter(role -> role.value.equals(value))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Rol de cuenta desconocido: " + value));
    }
}
