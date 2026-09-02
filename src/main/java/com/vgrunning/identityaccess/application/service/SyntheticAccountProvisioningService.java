package com.vgrunning.identityaccess.application.service;

import com.vgrunning.identityaccess.application.port.in.ProvisionSyntheticAccountsUseCase;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.port.out.SyntheticAccountRepository;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.EmailAddress;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Coordina el aprovisionamiento idempotente de datos sintéticos. */
@RequiredArgsConstructor
public final class SyntheticAccountProvisioningService
        implements ProvisionSyntheticAccountsUseCase {
    private static final UUID ADMINISTRATOR_ID =
            UUID.fromString("4dc63e62-4f24-44ad-bce3-4a60f37f9c59");
    private static final UUID RUNNER_ID = UUID.fromString("aa596d37-9a7e-4dd1-b4bb-e92986171292");

    private final SyntheticAccountRepository accounts;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional
    public void provision(Command command) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        provision(
                ADMINISTRATOR_ID,
                AccountRole.ADMINISTRADOR,
                "administrator@running-coach.invalid",
                command.administratorPassword(),
                now);
        provision(
                RUNNER_ID,
                AccountRole.CORREDOR,
                "runner@running-coach.invalid",
                command.runnerPassword(),
                now);
    }

    private void provision(
            UUID id, AccountRole role, String presentationEmail, String password, OffsetDateTime now) {
        String canonicalEmail = EmailAddress.from(presentationEmail).canonicalValue();
        accounts.provision(
                id,
                role,
                presentationEmail,
                canonicalEmail,
                passwordHasher.hash(Normalizer.normalize(password, Normalizer.Form.NFC)),
                now);
    }
}
