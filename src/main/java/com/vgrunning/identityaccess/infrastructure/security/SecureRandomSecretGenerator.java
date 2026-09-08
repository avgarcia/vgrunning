package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.application.port.out.SecretGenerator;
import java.security.SecureRandom;
import java.util.Base64;

/** Genera secretos de 32 bytes con un generador aleatorio criptográficamente seguro. */
public final class SecureRandomSecretGenerator implements SecretGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SECRET_LENGTH_BYTES = 32;

    @Override
    public String generateUrlSafeSecret() {
        byte[] bytes = new byte[SECRET_LENGTH_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
