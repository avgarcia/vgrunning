package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import java.util.Optional;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/** Implementación Argon2id con los parámetros mínimos aprobados para cuentas locales. */
public final class Argon2PasswordHasher implements PasswordHasher {
    private static final String DUMMY_PASSWORD = "not-a-real-password";
    private final Argon2PasswordEncoder encoder;
    private final String dummyHash;

    public Argon2PasswordHasher(Argon2PasswordEncoder encoder) {
        this.encoder = encoder;
        this.dummyHash = encoder.encode(DUMMY_PASSWORD);
    }

    @Override
    public boolean matchesForAuthentication(String password, Optional<String> encodedPassword) {
        return encoder.matches(password, encodedPassword.orElse(dummyHash));
    }

    @Override
    public String hash(String password) {
        return encoder.encode(password);
    }

    @Override
    public boolean needsRehash(String encodedPassword) {
        return encoder.upgradeEncoding(encodedPassword);
    }
}
