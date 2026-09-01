package com.vgrunning.identityaccess.application;

import com.vgrunning.identityaccess.application.port.out.CurrentTimeProvider;
import com.vgrunning.identityaccess.application.port.out.IdentityAccessRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.EmailAddress;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Provisiona solo las cuentas explícitamente sintéticas habilitadas para desarrollo local. */
public class SyntheticAccountProvisioner {
    private final IdentityAccessRepository repository;
    private final CurrentTimeProvider timeProvider;
    private final PasswordHasher passwordHasher;

    public SyntheticAccountProvisioner(
            IdentityAccessRepository repository,
            CurrentTimeProvider timeProvider,
            PasswordHasher passwordHasher) {
        this.repository = repository;
        this.timeProvider = timeProvider;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public void provision(UUID id, AccountRole role, String email, String password) {
        String canonicalEmail = EmailAddress.from(email).canonicalValue();
        repository.provisionSyntheticAccount(
                id,
                role,
                email,
                canonicalEmail,
                passwordHasher.hash(PasswordNormalizer.normalize(password)),
                timeProvider.now());
    }
}
