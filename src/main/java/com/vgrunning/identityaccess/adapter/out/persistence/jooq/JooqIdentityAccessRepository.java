package com.vgrunning.identityaccess.adapter.out.persistence.jooq;

import static org.vgrunning.generated.jooq.identity_access.tables.AccessSession.ACCESS_SESSION;
import static org.vgrunning.generated.jooq.identity_access.tables.Account.ACCOUNT;
import static org.vgrunning.generated.jooq.identity_access.tables.AccountEmail.ACCOUNT_EMAIL;
import static org.vgrunning.generated.jooq.identity_access.tables.AuthRateLimitBucket.AUTH_RATE_LIMIT_BUCKET;
import static org.vgrunning.generated.jooq.identity_access.tables.SecurityEvent.SECURITY_EVENT;

import com.vgrunning.identityaccess.application.SessionIdentity;
import com.vgrunning.identityaccess.application.port.out.IdentityAccessRepository;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import com.vgrunning.identityaccess.domain.securityevent.SecurityEventRetention;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/** Adaptador jOOQ propietario de las tablas del esquema {@code identity_access}. */
@Repository
public class JooqIdentityAccessRepository implements IdentityAccessRepository {
    private final DSLContext jooq;
    private final SecurityEventRetention securityEventRetention;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "DSLContext es una dependencia inyectada y gestionada por Spring/jOOQ.")
    public JooqIdentityAccessRepository(
            DSLContext jooq, SecurityEventRetention securityEventRetention) {
        this.jooq = jooq;
        this.securityEventRetention = securityEventRetention;
    }

    @Override
    public Optional<CredentialAccount> findCredentialAccount(String canonicalEmail) {
        return jooq.select(
                        ACCOUNT.ID,
                        ACCOUNT.ROLE,
                        ACCOUNT.STATUS,
                        ACCOUNT.PASSWORD_HASH,
                        ACCOUNT.VERSION)
                .from(ACCOUNT)
                .join(ACCOUNT_EMAIL)
                .on(ACCOUNT_EMAIL.ACCOUNT_ID.eq(ACCOUNT.ID))
                .where(ACCOUNT_EMAIL.CANONICAL_EMAIL.eq(canonicalEmail))
                .and(ACCOUNT_EMAIL.USAGE.eq("current"))
                .and(ACCOUNT_EMAIL.RELEASED_AT.isNull())
                .fetchOptional()
                .map(
                        record ->
                                new CredentialAccount(
                                        record.get(ACCOUNT.ID),
                                        AccountRole.fromValue(record.get(ACCOUNT.ROLE)),
                                        AccountStatus.fromValue(record.get(ACCOUNT.STATUS)),
                                        record.get(ACCOUNT.PASSWORD_HASH),
                                        record.get(ACCOUNT.VERSION)));
    }

    @Override
    public Optional<OffsetDateTime> limitedUntil(
            byte[] accountKey, byte[] ipKey, OffsetDateTime now, int accountLimit, int ipLimit) {
        OffsetDateTime windowStart = fixedWindowStart(now);
        OffsetDateTime windowEnd = windowStart.plusMinutes(15);
        int accountFailures = countFailures("account_login_failure", accountKey, windowStart);
        int ipFailures = countFailures("ip_login_failure", ipKey, windowStart);
        if (accountFailures >= accountLimit || ipFailures >= ipLimit) {
            return Optional.of(windowEnd);
        }
        return Optional.empty();
    }

    @Override
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

    @Override
    public void rehashPassword(
            CredentialAccount account,
            String replacementHash,
            OffsetDateTime now,
            UUID correlationId) {
        int updated =
                jooq.update(ACCOUNT)
                        .set(ACCOUNT.PASSWORD_HASH, replacementHash)
                        .set(ACCOUNT.PASSWORD_CHANGED_AT, now)
                        .set(ACCOUNT.UPDATED_AT, now)
                        .set(ACCOUNT.VERSION, ACCOUNT.VERSION.plus(1L))
                        .where(ACCOUNT.ID.eq(account.id()))
                        .and(ACCOUNT.VERSION.eq(account.version()))
                        .execute();
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

    @Override
    public SessionIdentity createSession(
            CredentialAccount account,
            byte[] verifierHash,
            OffsetDateTime now,
            OffsetDateTime absoluteExpiresAt,
            UUID correlationId) {
        UUID sessionId = UUID.randomUUID();
        jooq.insertInto(ACCESS_SESSION)
                .set(ACCESS_SESSION.ID, sessionId)
                .set(ACCESS_SESSION.ACCOUNT_ID, account.id())
                .set(ACCESS_SESSION.VERIFIER_SHA256, verifierHash)
                .set(ACCESS_SESSION.CREATED_AT, now)
                .set(ACCESS_SESSION.LAST_USED_AT, now)
                .set(ACCESS_SESSION.ABSOLUTE_EXPIRES_AT, absoluteExpiresAt)
                .execute();
        insertSecurityEvent(
                "session_created",
                "success",
                "account",
                account.id(),
                account.id(),
                sessionId,
                now,
                correlationId);
        return new SessionIdentity(sessionId, account.id(), account.role(), account.status());
    }

    @Override
    public Optional<SessionIdentity> authenticate(
            byte[] verifierHash, OffsetDateTime now, Duration idleTimeout) {
        Optional<StoredSession> found =
                jooq.select(
                                ACCESS_SESSION.ID,
                                ACCESS_SESSION.ACCOUNT_ID,
                                ACCESS_SESSION.LAST_USED_AT,
                                ACCESS_SESSION.ABSOLUTE_EXPIRES_AT,
                                ACCOUNT.ROLE,
                                ACCOUNT.STATUS)
                        .from(ACCESS_SESSION)
                        .join(ACCOUNT)
                        .on(ACCOUNT.ID.eq(ACCESS_SESSION.ACCOUNT_ID))
                        .where(ACCESS_SESSION.VERIFIER_SHA256.eq(verifierHash))
                        .and(ACCESS_SESSION.REVOKED_AT.isNull())
                        .fetchOptional()
                        .map(JooqIdentityAccessRepository::toStoredSession);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        StoredSession session = found.orElseThrow();
        if (session.status() != AccountStatus.ACTIVE) {
            revoke(session.identity(), now, "account_inactive", UUID.randomUUID());
            return Optional.empty();
        }
        if (!now.isBefore(session.absoluteExpiresAt())
                || !now.isBefore(session.lastUsedAt().plus(idleTimeout))) {
            int revoked =
                    jooq.update(ACCESS_SESSION)
                            .set(ACCESS_SESSION.REVOKED_AT, now)
                            .set(ACCESS_SESSION.REVOCATION_REASON, "expired")
                            .where(ACCESS_SESSION.ID.eq(session.id()))
                            .and(ACCESS_SESSION.REVOKED_AT.isNull())
                            .execute();
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
                jooq.update(ACCESS_SESSION)
                        .set(ACCESS_SESSION.LAST_USED_AT, now)
                        .where(ACCESS_SESSION.ID.eq(session.id()))
                        .and(ACCESS_SESSION.REVOKED_AT.isNull())
                        .execute();
        return touched == 1 ? Optional.of(session.identity()) : Optional.empty();
    }

    @Override
    public void revoke(
            SessionIdentity session, OffsetDateTime now, String reason, UUID correlationId) {
        int revoked =
                jooq.update(ACCESS_SESSION)
                        .set(ACCESS_SESSION.REVOKED_AT, now)
                        .set(ACCESS_SESSION.REVOCATION_REASON, reason)
                        .where(ACCESS_SESSION.ID.eq(session.sessionId()))
                        .and(ACCESS_SESSION.REVOKED_AT.isNull())
                        .execute();
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

    @Override
    public void provisionSyntheticAccount(
            UUID accountId,
            AccountRole role,
            String presentationEmail,
            String canonicalEmail,
            String passwordHash,
            OffsetDateTime now) {
        Optional<SyntheticAccount> existing =
                jooq.select(ACCOUNT.ID, ACCOUNT.ROLE, ACCOUNT.STATUS)
                        .from(ACCOUNT)
                        .where(ACCOUNT.ID.eq(accountId))
                        .fetchOptional()
                        .map(
                                record ->
                                        new SyntheticAccount(
                                                record.get(ACCOUNT.ID),
                                                AccountRole.fromValue(record.get(ACCOUNT.ROLE)),
                                                AccountStatus.fromValue(
                                                        record.get(ACCOUNT.STATUS))));
        if (existing.isPresent()) {
            SyntheticAccount account = existing.orElseThrow();
            boolean emailExists =
                    jooq.fetchExists(
                            jooq.selectOne()
                                    .from(ACCOUNT_EMAIL)
                                    .where(ACCOUNT_EMAIL.ACCOUNT_ID.eq(accountId))
                                    .and(ACCOUNT_EMAIL.CANONICAL_EMAIL.eq(canonicalEmail))
                                    .and(ACCOUNT_EMAIL.USAGE.eq("current"))
                                    .and(ACCOUNT_EMAIL.RELEASED_AT.isNull()));
            if (account.role() != role
                    || account.status() != AccountStatus.ACTIVE
                    || !emailExists) {
                throw new IllegalStateException(
                        "Existe un conflicto con una cuenta sintética esperada.");
            }
            return;
        }
        int conflicts =
                jooq.fetchCount(
                        ACCOUNT_EMAIL,
                        ACCOUNT_EMAIL
                                .CANONICAL_EMAIL
                                .eq(canonicalEmail)
                                .and(ACCOUNT_EMAIL.RELEASED_AT.isNull()));
        if (conflicts != 0) {
            throw new IllegalStateException(
                    "El correo sintético ya está reservado por otra cuenta.");
        }
        jooq.insertInto(ACCOUNT)
                .set(ACCOUNT.ID, accountId)
                .set(ACCOUNT.ROLE, role.value())
                .set(ACCOUNT.STATUS, AccountStatus.ACTIVE.value())
                .set(ACCOUNT.PASSWORD_HASH, passwordHash)
                .set(ACCOUNT.CREATED_AT, now)
                .set(ACCOUNT.UPDATED_AT, now)
                .set(ACCOUNT.STATUS_CHANGED_AT, now)
                .set(ACCOUNT.PASSWORD_CHANGED_AT, now)
                .set(ACCOUNT.VERSION, 0L)
                .execute();
        jooq.insertInto(ACCOUNT_EMAIL)
                .set(ACCOUNT_EMAIL.ID, UUID.randomUUID())
                .set(ACCOUNT_EMAIL.ACCOUNT_ID, accountId)
                .set(ACCOUNT_EMAIL.PRESENTATION_EMAIL, presentationEmail)
                .set(ACCOUNT_EMAIL.CANONICAL_EMAIL, canonicalEmail)
                .set(ACCOUNT_EMAIL.USAGE, "current")
                .set(ACCOUNT_EMAIL.CREATED_AT, now)
                .set(ACCOUNT_EMAIL.UPDATED_AT, now)
                .set(ACCOUNT_EMAIL.CONFIRMED_AT, now)
                .execute();
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

    private int countFailures(String bucketType, byte[] key, OffsetDateTime windowStart) {
        return jooq.select(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT)
                .from(AUTH_RATE_LIMIT_BUCKET)
                .where(AUTH_RATE_LIMIT_BUCKET.BUCKET_TYPE.eq(bucketType))
                .and(AUTH_RATE_LIMIT_BUCKET.KEY_HMAC_SHA256.eq(key))
                .and(AUTH_RATE_LIMIT_BUCKET.WINDOW_STARTED_AT.eq(windowStart))
                .fetchOptional(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT)
                .orElse(0);
    }

    private int incrementFailures(
            String bucketType,
            byte[] key,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            OffsetDateTime now) {
        Integer failures =
                jooq.insertInto(AUTH_RATE_LIMIT_BUCKET)
                        .set(AUTH_RATE_LIMIT_BUCKET.BUCKET_TYPE, bucketType)
                        .set(AUTH_RATE_LIMIT_BUCKET.KEY_HMAC_SHA256, key)
                        .set(AUTH_RATE_LIMIT_BUCKET.WINDOW_STARTED_AT, windowStart)
                        .set(AUTH_RATE_LIMIT_BUCKET.WINDOW_ENDS_AT, windowEnd)
                        .set(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT, 1)
                        .set(AUTH_RATE_LIMIT_BUCKET.CREATED_AT, now)
                        .set(AUTH_RATE_LIMIT_BUCKET.UPDATED_AT, now)
                        .set(AUTH_RATE_LIMIT_BUCKET.PURGE_AFTER, windowEnd.plusDays(1))
                        .onConflict(
                                AUTH_RATE_LIMIT_BUCKET.BUCKET_TYPE,
                                AUTH_RATE_LIMIT_BUCKET.KEY_HMAC_SHA256,
                                AUTH_RATE_LIMIT_BUCKET.WINDOW_STARTED_AT)
                        .doUpdate()
                        .set(
                                AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT,
                                AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT.plus(1))
                        .set(AUTH_RATE_LIMIT_BUCKET.UPDATED_AT, now)
                        .returningResult(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT)
                        .fetchOne(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT);
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
        jooq.insertInto(SECURITY_EVENT)
                .set(SECURITY_EVENT.ID, UUID.randomUUID())
                .set(SECURITY_EVENT.OCCURRED_AT, now)
                .set(SECURITY_EVENT.RETENTION_UNTIL, securityEventRetention.retentionUntil(now))
                .set(SECURITY_EVENT.EVENT_TYPE, eventType)
                .set(SECURITY_EVENT.OUTCOME, outcome)
                .set(SECURITY_EVENT.ACTOR_CLASS, actorClass)
                .set(SECURITY_EVENT.ACTOR_ACCOUNT_ID, actorAccountId)
                .set(SECURITY_EVENT.AFFECTED_ACCOUNT_ID, affectedAccountId)
                .set(SECURITY_EVENT.ACCESS_SESSION_ID, sessionId)
                .set(SECURITY_EVENT.CORRELATION_ID, correlationId)
                .set(SECURITY_EVENT.METADATA, JSONB.valueOf("{}"))
                .execute();
    }

    private static StoredSession toStoredSession(Record record) {
        return new StoredSession(
                record.get(ACCESS_SESSION.ID),
                record.get(ACCESS_SESSION.ACCOUNT_ID),
                record.get(ACCESS_SESSION.LAST_USED_AT),
                record.get(ACCESS_SESSION.ABSOLUTE_EXPIRES_AT),
                AccountRole.fromValue(record.get(ACCOUNT.ROLE)),
                AccountStatus.fromValue(record.get(ACCOUNT.STATUS)));
    }

    private static OffsetDateTime fixedWindowStart(OffsetDateTime now) {
        long epochSecond = now.toEpochSecond();
        return Instant.ofEpochSecond(epochSecond - Math.floorMod(epochSecond, 15 * 60L))
                .atOffset(ZoneOffset.UTC);
    }

    private record StoredSession(
            UUID id,
            UUID accountId,
            OffsetDateTime lastUsedAt,
            OffsetDateTime absoluteExpiresAt,
            AccountRole role,
            AccountStatus status) {
        SessionIdentity identity() {
            return new SessionIdentity(id, accountId, role, status);
        }
    }

    private record SyntheticAccount(UUID id, AccountRole role, AccountStatus status) {}
}
