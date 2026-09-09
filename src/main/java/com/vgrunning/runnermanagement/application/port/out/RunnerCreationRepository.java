package com.vgrunning.runnermanagement.application.port.out;

import com.vgrunning.runnermanagement.domain.Runner;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de perfil, auditoría e idempotencia del alta. */
public interface RunnerCreationRepository {
    Optional<StoredCreation> reserve(UUID administratorId, UUID idempotencyKey, byte[] fingerprint);

    Runner create(NewRunner runner);

    void complete(UUID administratorId, UUID idempotencyKey, Runner runner);

    // La huella se clona al construir y al leer, y equals/hashCode comparan su contenido:
    // el footgun que evita esta regla ya está cubierto explícitamente más abajo.
    @SuppressWarnings("ArrayRecordComponent")
    record StoredCreation(byte[] fingerprint, Runner runner) {
        public StoredCreation {
            fingerprint = Objects.requireNonNull(fingerprint).clone();
            Objects.requireNonNull(runner);
        }

        @Override
        public byte[] fingerprint() {
            return fingerprint.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StoredCreation that
                    && Arrays.equals(fingerprint, that.fingerprint)
                    && runner.equals(that.runner);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(fingerprint), runner);
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
