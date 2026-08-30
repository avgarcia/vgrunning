package com.vgrunning.identityaccess.application;

import java.text.Normalizer;
import org.springframework.stereotype.Component;

/** Normaliza exactamente los identificadores permitidos antes de verificarlos o derivar HMAC. */
@Component
public class IdentityNormalizer {

    public String canonicalEmail(String email) {
        return Normalizer.normalize(email.strip(), Normalizer.Form.NFC)
                .toLowerCase(java.util.Locale.ROOT);
    }

    public String normalizedPassword(String password) {
        return Normalizer.normalize(password, Normalizer.Form.NFC);
    }
}
