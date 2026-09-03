package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import static org.vgrunning.generated.jooq.identity_access.tables.Account.ACCOUNT;
import static org.vgrunning.generated.jooq.identity_access.tables.AccountEmail.ACCOUNT_EMAIL;

import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Salida jOOQ de credenciales y estado de cuenta. */
@Repository
@RequiredArgsConstructor
public class JooqAccountRepository implements AccountRepository {
    private final DSLContext jooq;
    private final AccountPersistenceMapper mapper;

    @Override
    public Optional<CredentialAccount> findCredentialAccount(String canonicalEmail) {
        return jooq.selectFrom(ACCOUNT)
                .whereExists(
                        jooq.selectOne()
                                .from(ACCOUNT_EMAIL)
                                .where(ACCOUNT_EMAIL.ACCOUNT_ID.eq(ACCOUNT.ID))
                                .and(ACCOUNT_EMAIL.CANONICAL_EMAIL.eq(canonicalEmail))
                                .and(ACCOUNT_EMAIL.USAGE.eq("current"))
                                .and(ACCOUNT_EMAIL.RELEASED_AT.isNull()))
                .fetchOptional(mapper::toCredentialAccount);
    }

    @Override
    public boolean updatePasswordHash(CredentialAccount account, String replacementHash) {
        return jooq.update(ACCOUNT)
                        .set(ACCOUNT.PASSWORD_HASH, replacementHash)
                        .set(ACCOUNT.PASSWORD_CHANGED_AT, DSL.currentOffsetDateTime())
                        .set(ACCOUNT.UPDATED_AT, DSL.currentOffsetDateTime())
                        .set(ACCOUNT.VERSION, ACCOUNT.VERSION.plus(1L))
                        .where(ACCOUNT.ID.eq(account.id()))
                        .and(ACCOUNT.VERSION.eq(account.version()))
                        .execute()
                == 1;
    }
}
