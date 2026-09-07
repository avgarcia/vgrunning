package com.vgrunning.identityaccess.infrastructure.configuration.synthetic;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credenciales exclusivamente sintéticas del perfil local opt-in. */
@ConfigurationProperties(prefix = "pmv.identity.synthetic")
public record SyntheticAccountProperties(String administratorPassword, String runnerPassword) {
    public SyntheticAccountProperties {
        if (administratorPassword == null || administratorPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "pmv.identity.synthetic.administrator-password es obligatoria.");
        }
        if (runnerPassword == null || runnerPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "pmv.identity.synthetic.runner-password es obligatoria.");
        }
    }
}
