package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import static org.vgrunning.generated.jooq.identity_access.tables.AccessChallenge.ACCESS_CHALLENGE;
import static org.vgrunning.generated.jooq.identity_access.tables.Account.ACCOUNT;
import static org.vgrunning.generated.jooq.identity_access.tables.AccountEmail.ACCOUNT_EMAIL;
import static org.vgrunning.generated.jooq.identity_access.tables.AdultDeclaration.ADULT_DECLARATION;
import static org.vgrunning.generated.jooq.identity_access.tables.InvitationAcceptance.INVITATION_ACCEPTANCE;

import com.vgrunning.identityaccess.application.exception.InvitationNotAvailableException;
import com.vgrunning.identityaccess.application.port.out.InvitationRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Persistencia jOOQ que bloquea y consume una invitación de activación una sola vez. */
@Repository
@RequiredArgsConstructor
public class JooqInvitationRepository implements InvitationRepository {
    private final DSLContext jooq;
    private final InvitationAcceptancePersistenceMapper mapper;

    @Override
    public Optional<ActivationInvitation> findAvailable(UUID invitationId) {
        return jooq.selectFrom(ACCESS_CHALLENGE)
                .where(ACCESS_CHALLENGE.ID.eq(invitationId))
                .and(ACCESS_CHALLENGE.PURPOSE.eq("activation"))
                .and(ACCESS_CHALLENGE.CONSUMED_AT.isNull())
                .and(ACCESS_CHALLENGE.REPLACED_AT.isNull())
                .and(ACCESS_CHALLENGE.EXPIRES_AT.gt(DSL.currentOffsetDateTime()))
                .andExists(
                        jooq.selectOne()
                                .from(ACCOUNT)
                                .where(ACCOUNT.ID.eq(ACCESS_CHALLENGE.ACCOUNT_ID))
                                .and(ACCOUNT.STATUS.eq("pending_activation")))
                .forUpdate()
                .fetchOptional(mapper::toActivationInvitation);
    }

    @Override
    public UUID accept(ActivationInvitation invitation, String passwordHash, UUID correlationId) {
        int accountUpdated =
                jooq.update(ACCOUNT)
                        .set(ACCOUNT.PASSWORD_HASH, passwordHash)
                        .set(ACCOUNT.STATUS, "active")
                        .set(ACCOUNT.PASSWORD_CHANGED_AT, DSL.currentOffsetDateTime())
                        .set(ACCOUNT.STATUS_CHANGED_AT, DSL.currentOffsetDateTime())
                        .set(ACCOUNT.UPDATED_AT, DSL.currentOffsetDateTime())
                        .set(ACCOUNT.VERSION, ACCOUNT.VERSION.plus(1L))
                        .where(ACCOUNT.ID.eq(invitation.accountId()))
                        .and(ACCOUNT.STATUS.eq("pending_activation"))
                        .execute();
        int challengeUpdated =
                jooq.update(ACCESS_CHALLENGE)
                        .set(ACCESS_CHALLENGE.CONSUMED_AT, DSL.currentOffsetDateTime())
                        .where(ACCESS_CHALLENGE.ID.eq(invitation.id()))
                        .and(ACCESS_CHALLENGE.CONSUMED_AT.isNull())
                        .and(ACCESS_CHALLENGE.REPLACED_AT.isNull())
                        .and(ACCESS_CHALLENGE.EXPIRES_AT.gt(DSL.currentOffsetDateTime()))
                        .execute();
        if (accountUpdated != 1 || challengeUpdated != 1) {
            throw new InvitationNotAvailableException();
        }
        jooq.update(ACCOUNT_EMAIL)
                .set(ACCOUNT_EMAIL.CONFIRMED_AT, DSL.currentOffsetDateTime())
                .set(ACCOUNT_EMAIL.UPDATED_AT, DSL.currentOffsetDateTime())
                .where(ACCOUNT_EMAIL.ACCOUNT_ID.eq(invitation.accountId()))
                .and(ACCOUNT_EMAIL.USAGE.eq("current"))
                .and(ACCOUNT_EMAIL.RELEASED_AT.isNull())
                .execute();
        UUID acceptanceId = UUID.randomUUID();
        jooq.insertInto(ADULT_DECLARATION)
                .set(ADULT_DECLARATION.ID, UUID.randomUUID())
                .set(ADULT_DECLARATION.ACCOUNT_ID, invitation.accountId())
                .set(ADULT_DECLARATION.ACTOR_KIND, "invitee")
                .set(ADULT_DECLARATION.ORIGIN, "initial_activation")
                .set(ADULT_DECLARATION.DECLARED_AT, DSL.currentOffsetDateTime())
                .set(ADULT_DECLARATION.TEXT_VERSION, "ux-02-v0.1")
                .execute();
        jooq.insertInto(INVITATION_ACCEPTANCE)
                .set(INVITATION_ACCEPTANCE.ID, acceptanceId)
                .set(INVITATION_ACCEPTANCE.CHALLENGE_ID, invitation.id())
                .set(INVITATION_ACCEPTANCE.ACCOUNT_ID, invitation.accountId())
                .set(INVITATION_ACCEPTANCE.ACCEPTED_AT, DSL.currentOffsetDateTime())
                .set(INVITATION_ACCEPTANCE.CORRELATION_ID, correlationId)
                .execute();
        return acceptanceId;
    }
}
