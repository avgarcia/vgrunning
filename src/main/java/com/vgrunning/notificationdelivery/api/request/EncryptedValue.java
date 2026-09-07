package com.vgrunning.notificationdelivery.api.request;

import java.util.Objects;

/** Resultado AEAD que no expone los arrays mutables de nonce y texto cifrado. */
public final class EncryptedValue {
    private final String keyId;
    private final byte[] nonce;
    private final byte[] ciphertext;

    public EncryptedValue(String keyId, byte[] nonce, byte[] ciphertext) {
        this.keyId = Objects.requireNonNull(keyId);
        this.nonce = Objects.requireNonNull(nonce).clone();
        this.ciphertext = Objects.requireNonNull(ciphertext).clone();
    }

    public String getKeyId() {
        return keyId;
    }

    public byte[] getNonce() {
        return nonce.clone();
    }

    public byte[] getCiphertext() {
        return ciphertext.clone();
    }
}
