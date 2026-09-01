package com.vgrunning.identityaccess.adapter.out.persistence.jooq;

import static org.vgrunning.generated.jooq.identity_access.tables.Account.ACCOUNT;
import static org.vgrunning.generated.jooq.identity_access.tables.AccountEmail.ACCOUNT_EMAIL;

import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Adaptador jOOQ de credenciales y estado de cuenta. */
@Repository
@RequiredArgsConstructor
public class JooqAccountRepository implements AccountRepository {
    private final DSLContext jooq;

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
                .fetchOptional(
                        record ->
                                CredentialAccount.restore(
                                        record.get(ACCOUNT.ID),
                                        AccountRole.fromValue(record.get(ACCOUNT.ROLE)),
                                        AccountStatus.fromValue(record.get(ACCOUNT.STATUS)),
                                        record.get(ACCOUNT.PASSWORD_HASH),
                                        record.get(ACCOUNT.VERSION)));
    }

    @Override
    public boolean updatePasswordHash(
            CredentialAccount account, String replacementHash, OffsetDateTime changedAt) {
        return jooq.update(ACCOUNT)
                        .set(ACCOUNT.PASSWORD_HASH, replacementHash)
                        .set(ACCOUNT.PASSWORD_CHANGED_AT, changedAt)
                        .set(ACCOUNT.UPDATED_AT, changedAt)
                        .set(ACCOUNT.VERSION, ACCOUNT.VERSION.plus(1L))
                        .where(ACCOUNT.ID.eq(account.id()))
                        .and(ACCOUNT.VERSION.eq(account.version()))
                        .execute()
                == 1;
    }
}
