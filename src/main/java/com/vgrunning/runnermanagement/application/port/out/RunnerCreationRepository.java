package com.vgrunning.runnermanagement.application.port.out;

import java.beans.ConstructorProperties;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de perfil, auditoría e idempotencia del alta. */
public interface RunnerCreationRepository {
    Reservation reserve(UUID administratorId, UUID idempotencyKey, byte[] fingerprint);

    StoredRunner create(NewRunner runner);

    void complete(UUID administratorId, UUID idempotencyKey, StoredRunner runner);

    record Reservation(Optional<StoredCreation> existing) {}

    final class StoredCreation {
        private final byte[] fingerprint;
        private final StoredRunner runner;

        @ConstructorProperties({"fingerprint", "runner"})
        public StoredCreation(byte[] fingerprint, StoredRunner runner) {
            this.fingerprint = java.util.Objects.requireNonNull(fingerprint).clone();
            this.runner = java.util.Objects.requireNonNull(runner);
        }

        public byte[] fingerprint() {
            return fingerprint.clone();
        }

        public StoredRunner runner() {
            return runner;
        }
    }

    record StoredRunner(UUID id, String givenName, String familyName, String status) {}

    record NewRunner(
            UUID runnerId,
            UUID accountId,
            String givenName,
            String familyName,
            UUID actorId,
            UUID correlationId,
            OffsetDateTime activationExpiresAt) {}
}
