package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import com.vgrunning.identityaccess.application.exception.EmailAlreadyReservedException;
import com.vgrunning.identityaccess.application.port.out.RunnerInvitationProvisioningRepository;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/** Persistencia jOOQ de la cuenta pendiente, desafío y declaración administrativa. */
@Repository
@RequiredArgsConstructor
public class JooqRunnerInvitationProvisioningRepository
        implements RunnerInvitationProvisioningRepository {
    private final DSLContext jooq;
    private final InvitationPersistenceMapper mapper;

    @Override
    public ProvisionedRunnerAccount provision(PendingRunnerInvitation invitation) {
        OffsetDateTime now =
                Objects.requireNonNull(
                        jooq.select(DSL.currentOffsetDateTime())
                                .fetchSingle(0, OffsetDateTime.class));
        OffsetDateTime expiresAt = now.plusDays(30);
        try {
            jooq.executeInsert(mapper.toAccountRecord(invitation, now));
            jooq.executeInsert(mapper.toAccountEmailRecord(invitation, now));
            jooq.executeInsert(mapper.toAccessChallengeRecord(invitation, now, expiresAt));
            jooq.executeInsert(mapper.toAdministratorDeclaration(invitation, now));
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyReservedException();
        } catch (DataAccessException exception) {
            if (isLiveEmailReservationViolation(exception)) {
                throw new EmailAlreadyReservedException();
            }
            throw exception;
        }
        return mapper.toProvisionedRunnerAccount(invitation, expiresAt);
    }

    private static boolean isLiveEmailReservationViolation(DataAccessException exception) {
        return "23505".equals(exception.sqlState());
    }
}
