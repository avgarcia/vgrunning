package com.vgrunning.runnermanagement.infrastructure.security;

import com.vgrunning.runnermanagement.application.port.out.DigestPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Calcula huellas SHA-256 sobre UTF-8. */
public final class Sha256DigestAdapter implements DigestPort {
    @Override
    public byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
