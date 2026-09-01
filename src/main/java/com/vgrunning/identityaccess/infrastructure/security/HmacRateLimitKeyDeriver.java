package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.application.port.out.RateLimitKeyDeriver;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA-256 con separación de dominio para los contadores de acceso. */
public final class HmacRateLimitKeyDeriver implements RateLimitKeyDeriver {
    private static final String ALGORITHM = "HmacSHA256";
    private final byte[] key;

    public HmacRateLimitKeyDeriver(String base64Key) {
        this.key = Base64.getDecoder().decode(base64Key);
        if (key.length < 32) {
            throw new IllegalArgumentException("La clave HMAC debe tener al menos 32 bytes.");
        }
    }

    @Override
    public byte[] accountKey(String canonicalEmail) {
        return derive("account-login-failure", canonicalEmail);
    }

    @Override
    public byte[] ipKey(String remoteAddress) {
        return derive("ip-login-failure", remoteAddress);
    }

    private byte[] derive(String domain, String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            mac.update(domain.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "No se pudo derivar la clave de límite de acceso.", exception);
        }
    }
}
