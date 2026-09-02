package com.vgrunning.identityaccess.infrastructure.configuration.synthetic;

import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.EmailAddress;
import com.vgrunning.identityaccess.infrastructure.output.persistence.jooq.JooqSyntheticAccountRepository;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/** Crea las cuentas exclusivamente sintéticas al arrancar el perfil local autorizado. */
@RequiredArgsConstructor
public final class SyntheticAccountsBootstrap implements ApplicationRunner {
    private static final UUID ADMINISTRATOR_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID RUNNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final String ADMINISTRATOR_EMAIL = "administrator@running-coach.invalid";
    private static final String RUNNER_EMAIL = "runner@running-coach.invalid";

    private final JooqSyntheticAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final SyntheticAccountProperties properties;

    /** Provisiona idempotentemente las dos cuentas disponibles solo en local y test. */
    @Override
    public void run(ApplicationArguments arguments) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        accounts.provisionAll(
                List.of(
                        provision(
                                ADMINISTRATOR_ID,
                                AccountRole.ADMINISTRADOR,
                                ADMINISTRATOR_EMAIL,
                                properties.administratorPassword(),
                                now),
                        provision(
                                RUNNER_ID,
                                AccountRole.CORREDOR,
                                RUNNER_EMAIL,
                                properties.runnerPassword(),
                                now)));
    }

    /** Delega la idempotencia y el rechazo de conflictos en la persistencia propietaria. */
    private JooqSyntheticAccountRepository.SyntheticAccountProvision provision(
            UUID accountId,
            AccountRole role,
            String email,
            String password,
            OffsetDateTime now) {
        EmailAddress canonicalEmail = EmailAddress.from(email);
        return new JooqSyntheticAccountRepository.SyntheticAccountProvision(
                accountId,
                role,
                email,
                canonicalEmail.canonicalValue(),
                passwordHasher.hash(Normalizer.normalize(password, Normalizer.Form.NFC)),
                now);
    }
}
