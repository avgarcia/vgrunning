package com.vgrunning.identityaccess.infrastructure.configuration.synthetic;

import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.infrastructure.output.persistence.jooq.JooqSyntheticAccountRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Perfil opt-in para datos de demostración; nunca aprovisiona cuentas fuera de local o test. */
@Configuration(proxyBeanMethods = false)
@Profile("synthetic-accounts")
@EnableConfigurationProperties(SyntheticAccountProperties.class)
public class SyntheticAccountsConfiguration {
    /** Compone el bootstrap únicamente cuando el perfil sintético también es local o test. */
    @Bean
    SyntheticAccountsBootstrap provisionSyntheticAccounts(
            Environment environment,
            JooqSyntheticAccountRepository accounts,
            PasswordHasher passwordHasher,
            SyntheticAccountProperties properties) {
        if (!environment.matchesProfiles("local | test")) {
            throw new IllegalStateException(
                    "El perfil synthetic-accounts solo se puede usar junto con local o test.");
        }
        return new SyntheticAccountsBootstrap(accounts, passwordHasher, properties);
    }
}
