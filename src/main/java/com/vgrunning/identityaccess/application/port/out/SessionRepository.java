package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.application.model.SessionIdentity;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de sesiones opacas; la vigencia se decide en aplicación. */
public interface SessionRepository {
    void create(
            SessionIdentity session,
            byte[] verifierHash,
            OffsetDateTime createdAt,
            OffsetDateTime absoluteExpiresAt);

    Optional<StoredSession> findByVerifierForUpdate(byte[] verifierHash);

    boolean touch(UUID sessionId, OffsetDateTime lastUsedAt);

    boolean revoke(UUID sessionId, OffsetDateTime revokedAt, String reason);

    record StoredSession(
            UUID id,
            UUID accountId,
            OffsetDateTime lastUsedAt,
            OffsetDateTime absoluteExpiresAt,
            AccountRole role,
            AccountStatus status) {
        public static StoredSession restore(
                UUID id,
                UUID accountId,
                OffsetDateTime lastUsedAt,
                OffsetDateTime absoluteExpiresAt,
                AccountRole role,
                AccountStatus status) {
            return new StoredSession(id, accountId, lastUsedAt, absoluteExpiresAt, role, status);
        }

        public SessionIdentity identity() {
            return SessionIdentity.restore(id, accountId, role, status);
        }
    }
}
