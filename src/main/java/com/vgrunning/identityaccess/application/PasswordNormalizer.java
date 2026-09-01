package com.vgrunning.identityaccess.application;

import java.text.Normalizer;

/** Normalización efímera aplicada a la contraseña antes de verificarla o codificarla. */
final class PasswordNormalizer {
    private PasswordNormalizer() {}

    static String normalize(String password) {
        return Normalizer.normalize(password, Normalizer.Form.NFC);
    }
}
