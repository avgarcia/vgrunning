package com.vgrunning.runnermanagement.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase.CreateRunner;
import com.vgrunning.runnermanagement.domain.Digest;
import org.junit.jupiter.api.Test;

class CreateRunnerUseCaseTest {

    @Test
    void fingerprintMatchesTheDigestOfTheNormalizedFields() {
        CreateRunner command = new CreateRunner("Lucía", "Martín", "lucia@example.invalid", true);

        assertThat(command.fingerprint())
                .isEqualTo(Digest.sha256("Lucía\nMartín\nlucia@example.invalid"));
    }

    @Test
    void fingerprintDistinguishesEmailCapitalization() {
        CreateRunner lowercase = new CreateRunner("Lucía", "Martín", "lucia@example.invalid", true);
        CreateRunner uppercase = new CreateRunner("Lucía", "Martín", "LUCIA@example.invalid", true);

        assertThat(lowercase.fingerprint()).isNotEqualTo(uppercase.fingerprint());
    }
}
