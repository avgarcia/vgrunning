package com.vgrunning.identityaccess.application;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Deriva claves no reversibles y separadas por dominio para los contadores de acceso. */
@Component
public class RateLimitKeyDeriver {
    private static final String ALGORITHM = "HmacSHA256";
    private final byte[] key;

    public RateLimitKeyDeriver(@Value("${pmv.identity.rate-limit-hmac-key}") String base64Key) {
        this.key = Base64.getDecoder().decode(base64Key);
        if (key.length < 32) {
            throw new IllegalArgumentException("La clave HMAC local debe tener al menos 32 bytes.");
        }
    }

    public byte[] accountKey(String canonicalEmail) {
        return derive("account-login-failure", canonicalEmail);
    }

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
