package com.vgrunning.identityaccess.domain;

import java.util.Objects;

/** Contenido cifrado con AEAD, propiedad del módulo que lo produce. */
public final class SealedPayload {
    private final String keyId;
    private final byte[] nonce;
    private final byte[] ciphertext;

    public SealedPayload(String keyId, byte[] nonce, byte[] ciphertext) {
        this.keyId = Objects.requireNonNull(keyId);
        this.nonce = Objects.requireNonNull(nonce).clone();
        this.ciphertext = Objects.requireNonNull(ciphertext).clone();
    }

    public String keyId() {
        return keyId;
    }

    public byte[] nonce() {
        return nonce.clone();
    }

    public byte[] ciphertext() {
        return ciphertext.clone();
    }
}
