package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.application.port.out.InvitationPayloadProtector;
import com.vgrunning.identityaccess.domain.SealedPayload;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Cifra sobres locales de invitación con AES-GCM antes de que lleguen a PostgreSQL. */
public final class AesGcmInvitationPayloadProtector implements InvitationPayloadProtector {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final SecretKeySpec key;

    public AesGcmInvitationPayloadProtector(String encodedKey) {
        byte[] decoded = Base64.getDecoder().decode(encodedKey);
        if (decoded.length != 32) {
            throw new IllegalStateException(
                    "La clave AES-GCM de invitaciones debe tener 32 bytes.");
        }
        this.key = new SecretKeySpec(decoded, "AES");
    }

    @Override
    public SealedPayload protect(String plaintext) {
        byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, nonce));
            return new SealedPayload(
                    "local-v1", nonce, cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se ha podido cifrar la invitación.", exception);
        }
    }
}
