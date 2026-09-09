package com.vgrunning.identityaccess.domain;

import java.util.Arrays;
import java.util.Objects;

/** Contenido cifrado con AEAD, propiedad del módulo que lo produce. */
// El nonce y el ciphertext se clonan al construir y al leer, y equals/hashCode comparan su
// contenido: el footgun que evita esta regla ya está cubierto explícitamente más abajo.
@SuppressWarnings("ArrayRecordComponent")
public record SealedPayload(String keyId, byte[] nonce, byte[] ciphertext) {
    public SealedPayload {
        Objects.requireNonNull(keyId);
        nonce = Objects.requireNonNull(nonce).clone();
        ciphertext = Objects.requireNonNull(ciphertext).clone();
    }

    @Override
    public byte[] nonce() {
        return nonce.clone();
    }

    @Override
    public byte[] ciphertext() {
        return ciphertext.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SealedPayload that
                && keyId.equals(that.keyId)
                && Arrays.equals(nonce, that.nonce)
                && Arrays.equals(ciphertext, that.ciphertext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyId, Arrays.hashCode(nonce), Arrays.hashCode(ciphertext));
    }
}
