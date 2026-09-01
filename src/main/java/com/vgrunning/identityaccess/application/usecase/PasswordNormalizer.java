package com.vgrunning.identityaccess.application.usecase;

import java.text.Normalizer;

/** Normalización efímera aplicada antes de verificar una contraseña. */
final class PasswordNormalizer {
    private PasswordNormalizer() {}

    static String normalize(String password) {
        return Normalizer.normalize(password, Normalizer.Form.NFC);
    }
}
