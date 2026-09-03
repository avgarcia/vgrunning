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
import org.vgrunning.generated.jooq.identity_access.tables.records.AccountRecord;

/** Salida jOOQ exclusiva del bootstrap sintético. */
@Repository
@RequiredArgsConstructor
public class JooqSyntheticAccountRepository {
    private final DSLContext jooq;
    private final AccountPersistenceMapper mapper;

    /** Persiste atómicamente las cuentas sintéticas verificadas por el bootstrap local. */
    public void provisionAll(List<SyntheticAccountProvision> provisions) {
        jooq.transaction(
                configuration -> {
                    DSLContext transaction = DSL.using(configuration);
                    provisions.forEach(provision -> provision(transaction, provision));
                });
    }

    /** Mantiene atómica la pareja cuenta-correo de una cuenta sintética. */
    private void provision(DSLContext jooq, SyntheticAccountProvision provision) {
        var existing = find(jooq, provision.accountId());
        if (existing.isPresent()) {
            verifyExisting(
                    jooq, existing.orElseThrow(), provision.role(), provision.canonicalEmail());
            return;
        }
        if (emailReserved(jooq, provision.canonicalEmail())) {
            throw new IllegalStateException(
                    "El correo sintético ya está reservado por otra cuenta.");
        }
        jooq.insertInto(ACCOUNT).set(mapper.toAccountRecord(provision)).execute();
        jooq.insertInto(ACCOUNT_EMAIL)
                .set(mapper.toAccountEmailRecord(provision, UUID.randomUUID()))
                .execute();
    }

    private static Optional<AccountRecord> find(DSLContext jooq, UUID accountId) {
        return jooq.selectFrom(ACCOUNT).where(ACCOUNT.ID.eq(accountId)).fetchOptional();
    }

    private static void verifyExisting(
            DSLContext jooq,
            AccountRecord account,
            AccountRole expectedRole,
            String canonicalEmail) {
        boolean emailExists =
                jooq.fetchExists(
                        jooq.selectOne()
                                .from(ACCOUNT_EMAIL)
                                .where(ACCOUNT_EMAIL.ACCOUNT_ID.eq(account.getId()))
                                .and(ACCOUNT_EMAIL.CANONICAL_EMAIL.eq(canonicalEmail))
                                .and(ACCOUNT_EMAIL.USAGE.eq("current"))
                                .and(ACCOUNT_EMAIL.RELEASED_AT.isNull()));
        if (!expectedRole.value().equals(account.getRole())
                || !AccountStatus.ACTIVE.value().equals(account.getStatus())
                || !emailExists) {
            throw new IllegalStateException(
                    "Existe un conflicto con una cuenta sintética esperada.");
        }
    }

    private static boolean emailReserved(DSLContext jooq, String canonicalEmail) {
        return jooq.fetchExists(
                jooq.selectOne()
                        .from(ACCOUNT_EMAIL)
                        .where(ACCOUNT_EMAIL.CANONICAL_EMAIL.eq(canonicalEmail))
                        .and(ACCOUNT_EMAIL.RELEASED_AT.isNull()));
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
