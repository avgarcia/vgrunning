package com.vgrunning.identityaccess.adapter.persistence;

import com.vgrunning.identityaccess.application.AuthenticatedSession;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Repository;

/** Adaptador JDBC propietario de las tablas de {@code identity_access}. */
@Repository
public class IdentityAccessRepository {
    private static final Duration RETENTION = Duration.ofDays(90);
    private final JdbcTemplate jdbc;
    private final String dummyPasswordHash;

    public IdentityAccessRepository(JdbcTemplate jdbc, Argon2PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-password");
    }

    public Optional<CredentialAccount> findCredentialAccount(String canonicalEmail) {
        return jdbc
                .query(
                        """
                        SELECT a.id, a.role, a.status, a.password_hash, a.version
                          FROM identity_access.account a
                          JOIN identity_access.account_email e ON e.account_id = a.id
                         WHERE e.canonical_email = ?
                           AND e.usage = 'current'
                           AND e.released_at IS NULL
                        """,
                        (resultSet, rowNumber) ->
                                new CredentialAccount(
                                        resultSet.getObject("id", UUID.class),
                                        resultSet.getString("role"),
                                        resultSet.getString("status"),
                                        resultSet.getString("password_hash"),
                                        resultSet.getLong("version")),
                        canonicalEmail)
                .stream()
                .findFirst();
    }

    public String dummyPasswordHash() {
        return dummyPasswordHash;
    }

    public Optional<OffsetDateTime> limitedUntil(
            byte[] accountKey, byte[] ipKey, OffsetDateTime now, int accountLimit, int ipLimit) {
        OffsetDateTime windowStart = fixedWindowStart(now);
        OffsetDateTime windowEnd = windowStart.plusMinutes(15);
        Integer accountFailures = countFailures("account_login_failure", accountKey, windowStart);
        Integer ipFailures = countFailures("ip_login_failure", ipKey, windowStart);
        if (accountFailures >= accountLimit || ipFailures >= ipLimit) {
            return Optional.of(windowEnd);
        }
        return Optional.empty();
    }

    public Optional<OffsetDateTime> recordFailedLogin(
            byte[] accountKey,
            byte[] ipKey,
            OffsetDateTime now,
            int accountLimit,
            int ipLimit,
            Duration window) {
        OffsetDateTime windowStart = fixedWindowStart(now);
        OffsetDateTime windowEnd = windowStart.plus(window);
        int accountFailures =
                incrementFailures("account_login_failure", accountKey, windowStart, windowEnd, now);
        int ipFailures = incrementFailures("ip_login_failure", ipKey, windowStart, windowEnd, now);
        if (accountFailures > accountLimit || ipFailures > ipLimit) {
            if (accountFailures == accountLimit + 1 || ipFailures == ipLimit + 1) {
                insertSecurityEvent(
                        "login_rate_limited",
                        "limited",
                        "anonymous",
                        null,
                        null,
                        null,
                        now,
                        UUID.randomUUID());
            }
            return Optional.of(windowEnd);
        }
        return Optional.empty();
    }

    public void rehashPassword(
            CredentialAccount account,
            String replacementHash,
            OffsetDateTime now,
            UUID correlationId) {
        int updated =
                jdbc.update(
                        """
                        UPDATE identity_access.account
                           SET password_hash = ?, password_changed_at = ?, updated_at = ?, version = version + 1
                         WHERE id = ? AND version = ?
                        """,
                        replacementHash,
                        now,
                        now,
                        account.id(),
                        account.version());
        if (updated != 1) {
            throw new IllegalStateException(
                    "La cuenta cambió mientras se actualizaba su hash de contraseña.");
        }
        insertSecurityEvent(
                "password_rehashed",
                "success",
                "account",
                account.id(),
                account.id(),
                null,
                now,
                correlationId);
    }

    public AuthenticatedSession createSession(
            CredentialAccount account,
            byte[] verifierHash,
            OffsetDateTime now,
            OffsetDateTime absoluteExpiresAt,
            UUID correlationId) {
        UUID sessionId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO identity_access.access_session (
                    id, account_id, verifier_sha256, created_at, last_used_at, absolute_expires_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                sessionId,
                account.id(),
                verifierHash,
                now,
                now,
                absoluteExpiresAt);
        insertSecurityEvent(
                "session_created",
                "success",
                "account",
                account.id(),
                account.id(),
                sessionId,
                now,
                correlationId);
        return new AuthenticatedSession(sessionId, account.id(), account.role(), account.status());
    }

    public Optional<AuthenticatedSession> authenticate(
            byte[] verifierHash, OffsetDateTime now, Duration idleTimeout) {
        Optional<StoredSession> found =
                jdbc
                        .query(
                                """
                                SELECT s.id, s.account_id, s.last_used_at, s.absolute_expires_at, a.role, a.status
                                  FROM identity_access.access_session s
                                  JOIN identity_access.account a ON a.id = s.account_id
                                 WHERE s.verifier_sha256 = ? AND s.revoked_at IS NULL
                                """,
                                (resultSet, rowNumber) ->
                                        new StoredSession(
                                                resultSet.getObject("id", UUID.class),
                                                resultSet.getObject("account_id", UUID.class),
                                                resultSet.getObject(
                                                        "last_used_at", OffsetDateTime.class),
                                                resultSet.getObject(
                                                        "absolute_expires_at",
                                                        OffsetDateTime.class),
                                                resultSet.getString("role"),
                                                resultSet.getString("status")),
                                verifierHash)
                        .stream()
                        .findFirst();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        StoredSession session = found.orElseThrow();
        if (!"active".equals(session.status())) {
            revoke(
                    new AuthenticatedSession(
                            session.id(), session.accountId(), session.role(), session.status()),
                    now,
                    "account_inactive",
                    UUID.randomUUID());
            return Optional.empty();
        }
        if (!now.isBefore(session.absoluteExpiresAt())
                || !now.isBefore(session.lastUsedAt().plus(idleTimeout))) {
            int revoked =
                    jdbc.update(
                            """
                            UPDATE identity_access.access_session
                               SET revoked_at = ?, revocation_reason = 'expired'
                             WHERE id = ? AND revoked_at IS NULL
                            """,
                            now,
                            session.id());
            if (revoked == 1) {
                insertSecurityEvent(
                        "session_expired",
                        "automatic",
                        "system",
                        null,
                        session.accountId(),
                        session.id(),
                        now,
                        UUID.randomUUID());
            }
            return Optional.empty();
        }
        int touched =
                jdbc.update(
                        """
                        UPDATE identity_access.access_session
                           SET last_used_at = ?
                         WHERE id = ? AND revoked_at IS NULL
                        """,
                        now,
                        session.id());
        if (touched != 1) {
            return Optional.empty();
        }
        return Optional.of(
                new AuthenticatedSession(
                        session.id(), session.accountId(), session.role(), session.status()));
    }

    public void revoke(
            AuthenticatedSession session, OffsetDateTime now, String reason, UUID correlationId) {
        int revoked =
                jdbc.update(
                        """
                        UPDATE identity_access.access_session
                           SET revoked_at = ?, revocation_reason = ?
                         WHERE id = ? AND revoked_at IS NULL
                        """,
                        now,
                        reason,
                        session.sessionId());
        if (revoked == 1) {
            insertSecurityEvent(
                    "session_revoked",
                    "success",
                    "account",
                    session.accountId(),
                    session.accountId(),
                    session.sessionId(),
                    now,
                    correlationId);
        }
    }

    public void provisionSyntheticAccount(
            UUID accountId,
            String role,
            String presentationEmail,
            String canonicalEmail,
            String passwordHash,
            OffsetDateTime now) {
        Optional<SyntheticAccount> existing =
                jdbc
                        .query(
                                "SELECT id, role, status FROM identity_access.account WHERE id = ?",
                                (resultSet, rowNumber) ->
                                        new SyntheticAccount(
                                                resultSet.getObject("id", UUID.class),
                                                resultSet.getString("role"),
                                                resultSet.getString("status")),
                                accountId)
                        .stream()
                        .findFirst();
        if (existing.isPresent()) {
            SyntheticAccount account = existing.orElseThrow();
            boolean emailExists =
                    Boolean.TRUE.equals(
                            jdbc.queryForObject(
                                    """
                                    SELECT EXISTS (
                                        SELECT 1 FROM identity_access.account_email
                                         WHERE account_id = ? AND canonical_email = ?
                                           AND usage = 'current' AND released_at IS NULL
                                    )
                                    """,
                                    Boolean.class,
                                    accountId,
                                    canonicalEmail));
            if (!role.equals(account.role())
                    || !"active".equals(account.status())
                    || !emailExists) {
                throw new IllegalStateException(
                        "Existe un conflicto con una cuenta sintética esperada.");
            }
            return;
        }
        Integer conflicts =
                jdbc.queryForObject(
                        """
                        SELECT count(*) FROM identity_access.account_email
                         WHERE canonical_email = ? AND released_at IS NULL
                        """,
                        Integer.class,
                        canonicalEmail);
        if (conflicts != null && conflicts != 0) {
            throw new IllegalStateException(
                    "El correo sintético ya está reservado por otra cuenta.");
        }
        jdbc.update(
                """
                INSERT INTO identity_access.account (
                    id, role, status, password_hash, created_at, updated_at, status_changed_at,
                    password_changed_at, version
                ) VALUES (?, ?, 'active', ?, ?, ?, ?, ?, 0)
                """,
                accountId,
                role,
                passwordHash,
                now,
                now,
                now,
                now);
        jdbc.update(
                """
                INSERT INTO identity_access.account_email (
                    id, account_id, presentation_email, canonical_email, usage, created_at, updated_at, confirmed_at
                ) VALUES (?, ?, ?, ?, 'current', ?, ?, ?)
                """,
                UUID.randomUUID(),
                accountId,
                presentationEmail,
                canonicalEmail,
                now,
                now,
                now);
        insertSecurityEvent(
                "synthetic_account_provisioned",
                "success",
                "system",
                null,
                accountId,
                null,
                now,
                UUID.randomUUID());
    }

    private Integer countFailures(String bucketType, byte[] key, OffsetDateTime windowStart) {
        return jdbc
                .query(
                        """
                        SELECT failure_count FROM identity_access.auth_rate_limit_bucket
                         WHERE bucket_type = ? AND key_hmac_sha256 = ? AND window_started_at = ?
                        """,
                        (resultSet, rowNumber) -> resultSet.getInt("failure_count"),
                        bucketType,
                        key,
                        windowStart)
                .stream()
                .findFirst()
                .orElse(0);
    }

    private int incrementFailures(
            String bucketType,
            byte[] key,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            OffsetDateTime now) {
        Integer failures =
                jdbc.queryForObject(
                        """
                        INSERT INTO identity_access.auth_rate_limit_bucket (
                            bucket_type, key_hmac_sha256, window_started_at, window_ends_at, failure_count,
                            created_at, updated_at, purge_after
                        ) VALUES (?, ?, ?, ?, 1, ?, ?, ?)
                        ON CONFLICT (bucket_type, key_hmac_sha256, window_started_at)
                        DO UPDATE SET failure_count = identity_access.auth_rate_limit_bucket.failure_count + 1,
                                      updated_at = EXCLUDED.updated_at
                        RETURNING failure_count
                        """,
                        Integer.class,
                        bucketType,
                        key,
                        windowStart,
                        windowEnd,
                        now,
                        now,
                        windowEnd.plusDays(1));
        if (failures == null) {
            throw new IllegalStateException("El contador de acceso no devolvió un valor.");
        }
        return failures;
    }

    private void insertSecurityEvent(
            String eventType,
            String outcome,
            String actorClass,
            UUID actorAccountId,
            UUID affectedAccountId,
            UUID sessionId,
            OffsetDateTime now,
            UUID correlationId) {
        jdbc.update(
                """
                INSERT INTO identity_access.security_event (
                    id, occurred_at, retention_until, event_type, outcome, actor_class,
                    actor_account_id, affected_account_id, access_session_id, correlation_id, metadata
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '{}'::jsonb)
                """,
                UUID.randomUUID(),
                now,
                now.plus(RETENTION),
                eventType,
                outcome,
                actorClass,
                actorAccountId,
                affectedAccountId,
                sessionId,
                correlationId);
    }

    private static OffsetDateTime fixedWindowStart(OffsetDateTime now) {
        long epochSecond = now.toEpochSecond();
        return Instant.ofEpochSecond(epochSecond - Math.floorMod(epochSecond, 15 * 60L))
                .atOffset(ZoneOffset.UTC);
    }

    public record CredentialAccount(
            UUID id, String role, String status, String passwordHash, long version) {}

    private record StoredSession(
            UUID id,
            UUID accountId,
            OffsetDateTime lastUsedAt,
            OffsetDateTime absoluteExpiresAt,
            String role,
            String status) {}

    private record SyntheticAccount(UUID id, String role, String status) {}
}
