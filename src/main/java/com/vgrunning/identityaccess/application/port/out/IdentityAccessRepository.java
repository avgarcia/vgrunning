package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.application.SessionIdentity;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Operaciones persistentes específicas de los casos de uso de acceso. */
public interface IdentityAccessRepository {
    Optional<CredentialAccount> findCredentialAccount(String canonicalEmail);

    Optional<OffsetDateTime> limitedUntil(
            byte[] accountKey, byte[] ipKey, OffsetDateTime now, int accountLimit, int ipLimit);

    Optional<OffsetDateTime> recordFailedLogin(
            byte[] accountKey,
            byte[] ipKey,
            OffsetDateTime now,
            int accountLimit,
            int ipLimit,
            Duration window);

    void rehashPassword(
            CredentialAccount account,
            String replacementHash,
            OffsetDateTime now,
            UUID correlationId);

    SessionIdentity createSession(
            CredentialAccount account,
            byte[] verifierHash,
            OffsetDateTime now,
            OffsetDateTime absoluteExpiresAt,
            UUID correlationId);

    Optional<SessionIdentity> authenticate(
            byte[] verifierHash, OffsetDateTime now, Duration idleTimeout);

    void revoke(SessionIdentity session, OffsetDateTime now, String reason, UUID correlationId);

    void provisionSyntheticAccount(
            UUID accountId,
            AccountRole role,
            String presentationEmail,
            String canonicalEmail,
            String passwordHash,
            OffsetDateTime now);

    record CredentialAccount(
            UUID id, AccountRole role, AccountStatus status, String passwordHash, long version) {}
}
