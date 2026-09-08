package com.vgrunning.runnermanagement.application.port.out;

import com.vgrunning.runnermanagement.domain.Runner;
import java.beans.ConstructorProperties;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de perfil, auditoría e idempotencia del alta. */
public interface RunnerCreationRepository {
    Optional<StoredCreation> reserve(UUID administratorId, UUID idempotencyKey, byte[] fingerprint);

    Runner create(NewRunner runner);

    void complete(UUID administratorId, UUID idempotencyKey, Runner runner);

    final class StoredCreation {
        private final byte[] fingerprint;
        private final Runner runner;

        @ConstructorProperties({"fingerprint", "runner"})
        public StoredCreation(byte[] fingerprint, Runner runner) {
            this.fingerprint = java.util.Objects.requireNonNull(fingerprint).clone();
            this.runner = java.util.Objects.requireNonNull(runner);
        }

        public byte[] fingerprint() {
            return fingerprint.clone();
        }

        public Runner runner() {
            return runner;
        }
    }

    record NewRunner(
            UUID runnerId,
            UUID accountId,
            String givenName,
            String familyName,
            UUID actorId,
            UUID correlationId,
            OffsetDateTime activationExpiresAt) {}
}
