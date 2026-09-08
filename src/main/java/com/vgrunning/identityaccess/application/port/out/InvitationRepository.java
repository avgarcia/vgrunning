package com.vgrunning.identityaccess.application.port.out;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de desafíos y de su aceptación atómica. */
public interface InvitationRepository {
    Optional<ActivationInvitation> findAvailable(UUID invitationId);

    UUID accept(ActivationInvitation invitation, String passwordHash, UUID correlationId);

    // El verificador se clona al construir y al leer, y equals/hashCode comparan su contenido:
    // el footgun que evita esta regla ya está cubierto explícitamente más abajo.
    @SuppressWarnings("ArrayRecordComponent")
    record ActivationInvitation(
            UUID id, UUID accountId, byte[] verifier, OffsetDateTime expiresAt) {
        public ActivationInvitation {
            Objects.requireNonNull(id);
            Objects.requireNonNull(accountId);
            verifier = Objects.requireNonNull(verifier).clone();
            Objects.requireNonNull(expiresAt);
        }

        @Override
        public byte[] verifier() {
            return verifier.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ActivationInvitation that
                    && id.equals(that.id)
                    && accountId.equals(that.accountId)
                    && Arrays.equals(verifier, that.verifier)
                    && expiresAt.equals(that.expiresAt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, accountId, Arrays.hashCode(verifier), expiresAt);
        }
    }
}
