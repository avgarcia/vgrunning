package com.vgrunning.identityaccess.application;

import com.vgrunning.identityaccess.adapter.persistence.IdentityAccessRepository;
import com.vgrunning.identityaccess.adapter.persistence.PostgresTimeProvider;
import java.util.UUID;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provisiona solo las cuentas explícitamente sintéticas habilitadas para desarrollo local. */
@Service
public class SyntheticAccountProvisioner {
    private final IdentityAccessRepository repository;
    private final PostgresTimeProvider timeProvider;
    private final IdentityNormalizer normalizer;
    private final Argon2PasswordEncoder passwordEncoder;

    public SyntheticAccountProvisioner(
            IdentityAccessRepository repository,
            PostgresTimeProvider timeProvider,
            IdentityNormalizer normalizer,
            Argon2PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.timeProvider = timeProvider;
        this.normalizer = normalizer;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void provision(UUID id, String role, String email, String password) {
        String canonicalEmail = normalizer.canonicalEmail(email);
        repository.provisionSyntheticAccount(
                id,
                role,
                email,
                canonicalEmail,
                passwordEncoder.encode(normalizer.normalizedPassword(password)),
                timeProvider.now());
    }
}
