package com.vgrunning.identityaccess.infrastructure.configuration.synthetic;

import com.vgrunning.identityaccess.application.port.out.DatabaseTransactionClock;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.port.out.SecurityEventRepository;
import com.vgrunning.identityaccess.application.port.out.SyntheticAccountRepository;
import com.vgrunning.identityaccess.application.securityevent.SecurityEvent;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.EmailAddress;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.transaction.support.TransactionTemplate;

/** Perfil opt-in para datos de demostración; nunca aprovisiona cuentas fuera de local o test. */
@Configuration(proxyBeanMethods = false)
@Profile("synthetic-accounts")
@EnableConfigurationProperties(SyntheticAccountProperties.class)
public class SyntheticAccountsConfiguration {
    private static final UUID ADMINISTRATOR_ID =
            UUID.fromString("4dc63e62-4f24-44ad-bce3-4a60f37f9c59");
    private static final UUID RUNNER_ID = UUID.fromString("aa596d37-9a7e-4dd1-b4bb-e92986171292");

    @Bean
    ApplicationRunner provisionSyntheticAccounts(
            Environment environment,
            SyntheticAccountRepository accounts,
            SecurityEventRepository events,
            DatabaseTransactionClock clock,
            PasswordHasher passwordHasher,
            TransactionTemplate transactions,
            SyntheticAccountProperties properties) {
        Set<String> profiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        if (!profiles.contains("local") && !profiles.contains("test")) {
            throw new IllegalStateException(
                    "El perfil synthetic-accounts solo se puede usar junto con local o test.");
        }
        String administratorHash =
                passwordHasher.hash(normalize(properties.administratorPassword()));
        String runnerHash = passwordHasher.hash(normalize(properties.runnerPassword()));
        return arguments ->
                transactions.executeWithoutResult(
                        status -> {
                            OffsetDateTime now = clock.now();
                            provision(
                                    accounts,
                                    events,
                                    ADMINISTRATOR_ID,
                                    AccountRole.ADMINISTRADOR,
                                    "administrator@running-coach.invalid",
                                    administratorHash,
                                    now);
                            provision(
                                    accounts,
                                    events,
                                    RUNNER_ID,
                                    AccountRole.CORREDOR,
                                    "runner@running-coach.invalid",
                                    runnerHash,
                                    now);
                        });
    }

    private static void provision(
            SyntheticAccountRepository accounts,
            SecurityEventRepository events,
            UUID id,
            AccountRole role,
            String email,
            String passwordHash,
            OffsetDateTime now) {
        String canonicalEmail = EmailAddress.from(email).canonicalValue();
        if (accounts.provision(id, role, email, canonicalEmail, passwordHash, now)) {
            events.append(SecurityEvent.syntheticAccountProvisioned(id, now, UUID.randomUUID()));
        }
    }

    private static String normalize(String password) {
        return Normalizer.normalize(password, Normalizer.Form.NFC);
    }
}
