package com.vgrunning.identityaccess.domain.account.valueobject;

import java.text.Normalizer;

/** Contraseña en claro normalizada y validada antes de hashearse. */
public record RawPassword(String value) {

    public RawPassword {
        if (value == null || value.length() < 12 || value.length() > 128) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre 12 y 128 caracteres.");
        }
    }

    /** Normaliza NFC y valida la longitud antes de aceptar la contraseña. */
    public static RawPassword from(String suppliedPassword) {
        return new RawPassword(Normalizer.normalize(suppliedPassword, Normalizer.Form.NFC));
    }
}
