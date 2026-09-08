package com.vgrunning.runnermanagement.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository.StoredCreation;
import com.vgrunning.runnermanagement.domain.Runner;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RunnerCreationRepositoryTest {
    private static final Runner RUNNER =
            new Runner(
                    UUID.fromString("50000000-0000-0000-0000-000000000001"),
                    "Lucía",
                    "Martín",
                    "pending_activation");

    @Test
    void clonesTheFingerprintOnConstructionAndOnRead() {
        byte[] fingerprint = {1, 2, 3};
        StoredCreation stored = new StoredCreation(fingerprint, RUNNER);

        fingerprint[0] = 9;
        stored.fingerprint()[0] = 9;

        assertThat(stored.fingerprint()).containsExactly(1, 2, 3);
    }

    @Test
    void equalsAndHashCodeCompareFingerprintContentNotReference() {
        StoredCreation first = new StoredCreation(new byte[] {1, 2, 3}, RUNNER);
        StoredCreation same = new StoredCreation(new byte[] {1, 2, 3}, RUNNER);
        StoredCreation differentFingerprint = new StoredCreation(new byte[] {9, 9, 9}, RUNNER);
        StoredCreation differentRunner =
                new StoredCreation(
                        new byte[] {1, 2, 3},
                        new Runner(UUID.randomUUID(), "Ana", "López", "pending_activation"));

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(first).isNotEqualTo(differentFingerprint);
        assertThat(first).isNotEqualTo(differentRunner);
        assertThat(first).isNotEqualTo("not-a-stored-creation");
    }

    @Test
    void rejectsMissingFields() {
        byte[] fingerprint = {1};
        assertThatThrownBy(() -> new StoredCreation(null, RUNNER))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new StoredCreation(fingerprint, null))
                .isInstanceOf(NullPointerException.class);
    }
}
