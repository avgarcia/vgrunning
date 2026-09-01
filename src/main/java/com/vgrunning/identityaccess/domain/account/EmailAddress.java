package com.vgrunning.identityaccess.domain.account;

import java.text.Normalizer;
import java.util.Locale;

/** Correo canónico usado para identidad, sin reglas específicas de proveedor. */
public record EmailAddress(String canonicalValue) {

    public EmailAddress {
        if (canonicalValue.isBlank()) {
            throw new IllegalArgumentException("El correo canónico no puede estar vacío.");
        }
    }

    public static EmailAddress from(String suppliedEmail) {
        String canonical =
                Normalizer.normalize(suppliedEmail.strip(), Normalizer.Form.NFC)
                        .toLowerCase(Locale.ROOT);
        return new EmailAddress(canonical);
    }
}
