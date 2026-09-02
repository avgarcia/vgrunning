package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import static org.vgrunning.generated.jooq.identity_access.tables.Account.ACCOUNT;
import static org.vgrunning.generated.jooq.identity_access.tables.AccountEmail.ACCOUNT_EMAIL;

import com.vgrunning.identityaccess.application.port.out.SyntheticAccountRepository;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Salida jOOQ exclusiva del bootstrap sintético. */
@Repository
@RequiredArgsConstructor
public class JooqSyntheticAccountRepository implements SyntheticAccountRepository {
    private final DSLContext jooq;

    @Override
    public boolean provision(
            UUID accountId,
            AccountRole role,
            String presentationEmail,
            String canonicalEmail,
            String passwordHash,
            OffsetDateTime now) {
        Optional<SyntheticAccount> existing = find(accountId);
        if (existing.isPresent()) {
            verifyExisting(existing.orElseThrow(), role, canonicalEmail);
            return false;
        }
        if (emailReserved(canonicalEmail)) {
            throw new IllegalStateException(
                    "El correo sintético ya está reservado por otra cuenta.");
        }
        insertAccount(accountId, role, passwordHash, now);
        insertEmail(accountId, presentationEmail, canonicalEmail, now);
        return true;
    }

    private Optional<SyntheticAccount> find(UUID accountId) {
        return jooq.select(ACCOUNT.ID, ACCOUNT.ROLE, ACCOUNT.STATUS)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(accountId))
                .fetchOptional(
                        record ->
                                SyntheticAccount.restore(
                                        record.get(ACCOUNT.ID),
                                        AccountRole.fromValue(record.get(ACCOUNT.ROLE)),
                                        AccountStatus.fromValue(record.get(ACCOUNT.STATUS))));
    }

    private void verifyExisting(
            SyntheticAccount account, AccountRole expectedRole, String canonicalEmail) {
        boolean emailExists =
                jooq.fetchExists(
                        jooq.selectOne()
                                .from(ACCOUNT_EMAIL)
                                .where(ACCOUNT_EMAIL.ACCOUNT_ID.eq(account.id()))
                                .and(ACCOUNT_EMAIL.CANONICAL_EMAIL.eq(canonicalEmail))
                                .and(ACCOUNT_EMAIL.USAGE.eq("current"))
                                .and(ACCOUNT_EMAIL.RELEASED_AT.isNull()));
        if (account.role() != expectedRole
                || account.status() != AccountStatus.ACTIVE
                || !emailExists) {
            throw new IllegalStateException(
                    "Existe un conflicto con una cuenta sintética esperada.");
        }
    }

    private boolean emailReserved(String canonicalEmail) {
        return jooq.fetchCount(
                        ACCOUNT_EMAIL,
                        ACCOUNT_EMAIL
                                .CANONICAL_EMAIL
                                .eq(canonicalEmail)
                                .and(ACCOUNT_EMAIL.RELEASED_AT.isNull()))
                != 0;
    }

    private void insertAccount(
            UUID accountId, AccountRole role, String passwordHash, OffsetDateTime now) {
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
    }

    private void insertEmail(
            UUID accountId, String presentationEmail, String canonicalEmail, OffsetDateTime now) {
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
    }

    private record SyntheticAccount(UUID id, AccountRole role, AccountStatus status) {
        static SyntheticAccount restore(UUID id, AccountRole role, AccountStatus status) {
            return new SyntheticAccount(id, role, status);
        }
    }
}
