package com.vgrunning.identityaccess.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Persistencia de desafíos y de su aceptación atómica. */
public interface InvitationRepository {
    Optional<ActivationInvitation> findAvailable(UUID invitationId);

    UUID accept(ActivationInvitation invitation, String passwordHash, UUID correlationId);

    final class ActivationInvitation {
        private final UUID id;
        private final UUID accountId;
        private final byte[] verifier;
        private final java.time.OffsetDateTime expiresAt;

        public ActivationInvitation(
                UUID id, UUID accountId, byte[] verifier, java.time.OffsetDateTime expiresAt) {
            this.id = java.util.Objects.requireNonNull(id);
            this.accountId = java.util.Objects.requireNonNull(accountId);
            this.verifier = java.util.Objects.requireNonNull(verifier).clone();
            this.expiresAt = java.util.Objects.requireNonNull(expiresAt);
        }

        public UUID id() {
            return id;
        }

        public UUID accountId() {
            return accountId;
        }

        public UUID getAccountId() {
            return accountId;
        }

        public byte[] getVerifier() {
            return verifier.clone();
        }

        public java.time.OffsetDateTime expiresAt() {
            return expiresAt;
        }
    }
}
