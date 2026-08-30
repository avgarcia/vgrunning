package com.vgrunning.identityaccess.adapter.synthetic;

import com.vgrunning.identityaccess.application.SyntheticAccountProvisioner;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Perfil opt-in para datos de demostración; nunca aprovisiona cuentas fuera de local o test. */
@Configuration
@Profile("synthetic-accounts")
class SyntheticAccountsConfiguration {
    private static final UUID ADMINISTRATOR_ID =
            UUID.fromString("4dc63e62-4f24-44ad-bce3-4a60f37f9c59");
    private static final UUID RUNNER_ID = UUID.fromString("aa596d37-9a7e-4dd1-b4bb-e92986171292");

    @Bean
    ApplicationRunner provisionSyntheticAccounts(
            Environment environment,
            SyntheticAccountProvisioner provisioner,
            @Value("${pmv.identity.synthetic.administrator-password}") String administratorPassword,
            @Value("${pmv.identity.synthetic.runner-password}") String runnerPassword) {
        Set<String> profiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        if (!profiles.contains("local") && !profiles.contains("test")) {
            throw new IllegalStateException(
                    "El perfil synthetic-accounts solo se puede usar junto con local o test.");
        }
        return arguments -> {
            provisioner.provision(
                    ADMINISTRATOR_ID,
                    "administrador",
                    "administrator@running-coach.invalid",
                    administratorPassword);
            provisioner.provision(
                    RUNNER_ID, "corredor", "runner@running-coach.invalid", runnerPassword);
        };
    }
}
