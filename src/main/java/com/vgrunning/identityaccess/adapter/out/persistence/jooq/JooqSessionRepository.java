package com.vgrunning.identityaccess.adapter.out.persistence.jooq;

import static org.vgrunning.generated.jooq.identity_access.tables.AccessSession.ACCESS_SESSION;
import static org.vgrunning.generated.jooq.identity_access.tables.Account.ACCOUNT;

import com.vgrunning.identityaccess.application.model.SessionIdentity;
import com.vgrunning.identityaccess.application.port.out.SessionRepository;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Adaptador jOOQ que persiste sesiones sin decidir su vigencia. */
@Repository
@RequiredArgsConstructor
public class JooqSessionRepository implements SessionRepository {
    private final DSLContext jooq;

    @Override
    public void create(
            SessionIdentity session,
            byte[] verifierHash,
            OffsetDateTime createdAt,
            OffsetDateTime absoluteExpiresAt) {
        jooq.insertInto(ACCESS_SESSION)
                .set(ACCESS_SESSION.ID, session.sessionId())
                .set(ACCESS_SESSION.ACCOUNT_ID, session.accountId())
                .set(ACCESS_SESSION.VERIFIER_SHA256, verifierHash)
                .set(ACCESS_SESSION.CREATED_AT, createdAt)
                .set(ACCESS_SESSION.LAST_USED_AT, createdAt)
                .set(ACCESS_SESSION.ABSOLUTE_EXPIRES_AT, absoluteExpiresAt)
                .execute();
    }

    @Override
    public Optional<StoredSession> findByVerifierForUpdate(byte[] verifierHash) {
        return jooq.select(
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
                .forUpdate()
                .fetchOptional(
                        record ->
                                StoredSession.restore(
                                        record.get(ACCESS_SESSION.ID),
                                        record.get(ACCESS_SESSION.ACCOUNT_ID),
                                        record.get(ACCESS_SESSION.LAST_USED_AT),
                                        record.get(ACCESS_SESSION.ABSOLUTE_EXPIRES_AT),
                                        AccountRole.fromValue(record.get(ACCOUNT.ROLE)),
                                        AccountStatus.fromValue(record.get(ACCOUNT.STATUS))));
    }

    @Override
    public boolean touch(UUID sessionId, OffsetDateTime lastUsedAt) {
        return jooq.update(ACCESS_SESSION)
                        .set(ACCESS_SESSION.LAST_USED_AT, lastUsedAt)
                        .where(ACCESS_SESSION.ID.eq(sessionId))
                        .and(ACCESS_SESSION.REVOKED_AT.isNull())
                        .execute()
                == 1;
    }

    @Override
    public boolean revoke(UUID sessionId, OffsetDateTime revokedAt, String reason) {
        return jooq.update(ACCESS_SESSION)
                        .set(ACCESS_SESSION.REVOKED_AT, revokedAt)
                        .set(ACCESS_SESSION.REVOCATION_REASON, reason)
                        .where(ACCESS_SESSION.ID.eq(sessionId))
                        .and(ACCESS_SESSION.REVOKED_AT.isNull())
                        .execute()
                == 1;
    }
}
