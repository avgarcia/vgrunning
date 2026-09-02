package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import static org.vgrunning.generated.jooq.identity_access.tables.Account.ACCOUNT;
import static org.vgrunning.generated.jooq.identity_access.tables.AccountEmail.ACCOUNT_EMAIL;

import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Salida jOOQ exclusiva del bootstrap sintético. */
@Repository
@RequiredArgsConstructor
public class JooqSyntheticAccountRepository {
    private final DSLContext jooq;

    /** Persiste atómicamente las cuentas sintéticas verificadas por el bootstrap local. */
    public void provisionAll(List<SyntheticAccountProvision> provisions) {
        jooq.transaction(
                configuration -> {
                    DSLContext transaction = DSL.using(configuration);
                    provisions.forEach(provision -> provision(transaction, provision));
                });
    }

    /** Mantiene atómica la pareja cuenta-correo de una cuenta sintética. */
    private static void provision(DSLContext jooq, SyntheticAccountProvision provision) {
        Optional<SyntheticAccount> existing = find(jooq, provision.accountId());
        if (existing.isPresent()) {
            verifyExisting(
                    jooq, existing.orElseThrow(), provision.role(), provision.canonicalEmail());
            return;
        }
        if (emailReserved(jooq, provision.canonicalEmail())) {
            throw new IllegalStateException(
                    "El correo sintético ya está reservado por otra cuenta.");
        }
        insertAccount(
                jooq,
                provision.accountId(),
                provision.role(),
                provision.passwordHash(),
                provision.now());
        insertEmail(
                jooq,
                provision.accountId(),
                provision.presentationEmail(),
                provision.canonicalEmail(),
                provision.now());
    }

    private static Optional<SyntheticAccount> find(DSLContext jooq, UUID accountId) {
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

    private static void verifyExisting(
            DSLContext jooq,
            SyntheticAccount account,
            AccountRole expectedRole,
            String canonicalEmail) {
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

    private static boolean emailReserved(DSLContext jooq, String canonicalEmail) {
        return jooq.fetchCount(
                        ACCOUNT_EMAIL,
                        ACCOUNT_EMAIL
                                .CANONICAL_EMAIL
                                .eq(canonicalEmail)
                                .and(ACCOUNT_EMAIL.RELEASED_AT.isNull()))
                != 0;
    }

    private static void insertAccount(
            DSLContext jooq,
            UUID accountId,
            AccountRole role,
            String passwordHash,
            OffsetDateTime now) {
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

    private static void insertEmail(
            DSLContext jooq,
            UUID accountId,
            String presentationEmail,
            String canonicalEmail,
            OffsetDateTime now) {
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

    /** Datos técnicos de una cuenta de demostración listos para persistir en una transacción. */
    public record SyntheticAccountProvision(
            UUID accountId,
            AccountRole role,
            String presentationEmail,
            String canonicalEmail,
            String passwordHash,
            OffsetDateTime now) {}
}
