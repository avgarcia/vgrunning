package com.vgrunning.runnermanagement.infrastructure.output.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository.NewRunner;
import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository.StoredRunner;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.vgrunning.generated.jooq.runner_management.tables.records.RunnerRecord;

class RunnerPersistenceMapperTest {
    private final RunnerPersistenceMapper mapper = new RunnerPersistenceMapperImpl();

    @Test
    void mapsRunnerCreationAndIdempotencyRepresentations() {
        UUID runnerId = UUID.randomUUID();
        NewRunner runner =
                new NewRunner(
                        runnerId,
                        UUID.randomUUID(),
                        "Lucía",
                        "Martín",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        OffsetDateTime.parse("2026-10-01T10:00:00Z"));

        RunnerRecord record = mapper.toRunnerRecord(runner);
        assertThat(record)
                .extracting("id", "accountId", "givenName", "familyName", "status")
                .containsExactly(
                        runnerId, runner.accountId(), "Lucía", "Martín", "pending_activation");
        assertThat(mapper.toStoredRunner(record))
                .isEqualTo(new StoredRunner(runnerId, "Lucía", "Martín", "pending_activation"));
        assertThat(mapper.toStoredCreation(new byte[] {1, 2}, record).fingerprint())
                .containsExactly(1, 2);
        assertThat(mapper.toStoredRunner(null)).isNull();
        assertThat(mapper.toStoredCreation(null, null)).isNull();
    }
}
