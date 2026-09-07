package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import java.util.Optional;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/** Implementación Argon2id con los parámetros mínimos aprobados para cuentas locales. */
public final class Argon2PasswordHasher implements PasswordHasher {
    private static final String DUMMY_PASSWORD = "not-a-real-password";
    private final Argon2PasswordEncoder encoder;
    private final String dummyHash;

    /** Crea el hasher y un hash ficticio para igualar el coste de verificaciones rechazadas. */
    public Argon2PasswordHasher(Argon2PasswordEncoder encoder) {
        this.encoder = encoder;
        this.dummyHash = encoder.encode(DUMMY_PASSWORD);
    }

    /**
     * Verifica siempre mediante Argon2 y solo autentica cuando existe un hash persistido válido.
     *
     * <p>Cuando falta el hash se compara contra uno ficticio para no revelar la causa por tiempo.
     */
    @Override
    public boolean matchesForAuthentication(String password, Optional<String> encodedPassword) {
        boolean matches = encoder.matches(password, encodedPassword.orElse(dummyHash));
        return encodedPassword.isPresent() && matches;
    }

    /** Calcula un hash Argon2id para una contraseña ya validada por el caso de uso. */
    @Override
    public String hash(String password) {
        return encoder.encode(password);
    }

    /** Indica si un hash persistido debe sustituirse con los parámetros vigentes de Argon2id. */
    @Override
    public boolean needsRehash(String encodedPassword) {
        return encoder.upgradeEncoding(encodedPassword);
    }
}
