package com.vgrunning.identityaccess.domain.account.valueobject;

import java.text.Normalizer;
import java.util.Locale;

/** Correo canónico usado para identidad, sin reglas específicas de proveedor. */
public record EmailAddress(String canonicalValue) {

    public EmailAddress {
        if (canonicalValue.isBlank()) {
            throw new IllegalArgumentException("El correo canónico no puede estar vacío.");
        }
        if (!canonicalValue.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("El correo canónico no tiene un formato válido.");
        }
    }

    /** Normaliza y valida un correo sin aplicar reglas de proveedores concretos. */
    public static EmailAddress from(String suppliedEmail) {
        return new EmailAddress(canonicalize(suppliedEmail));
    }

    /** Normaliza el identificador para que seguridad y autenticación usen la misma clave. */
    public static String canonicalize(String suppliedEmail) {
        return Normalizer.normalize(suppliedEmail.strip(), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
    }
}
