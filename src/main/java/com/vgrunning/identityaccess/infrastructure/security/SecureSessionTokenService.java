package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.application.port.out.SessionTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Genera 32 bytes aleatorios y persiste únicamente su verificador SHA-256. */
public final class SecureSessionTokenService implements SessionTokenService {
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public GeneratedSessionToken generate() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new GeneratedSessionToken(rawToken, verifier(rawToken));
    }

    @Override
    public byte[] verifier(String rawToken) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM.", exception);
        }
    }
}
