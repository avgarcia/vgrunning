package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.application.port.out.RunnerInvitationProvisioningRepository.PendingRunnerInvitation;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvitationPersistenceMapperTest {
    private final InvitationPersistenceMapper mapper = new InvitationPersistenceMapperImpl();

    @Test
    void mapsEveryRecordRequiredToProvisionAnInvitation() {
        PendingRunnerInvitation invitation = invitation();
        OffsetDateTime now = OffsetDateTime.parse("2026-09-05T10:00:00Z");
        OffsetDateTime expiresAt = now.plusDays(30);

        assertThat(mapper.toProvisionedRunnerAccount(invitation, expiresAt))
                .extracting("accountId", "invitationId", "activationExpiresAt")
                .containsExactly(
                        invitation.getAccountId(), invitation.getInvitationId(), expiresAt);
        assertThat(mapper.toAccountRecord(invitation, now))
                .extracting("id", "role", "status", "createdAt", "version")
                .containsExactly(
                        invitation.getAccountId(), "corredor", "pending_activation", now, 0L);
        assertThat(mapper.toAccountEmailRecord(invitation, now))
                .extracting("id", "accountId", "usage", "presentationEmail", "canonicalEmail")
                .containsExactly(
                        invitation.getEmailId(),
                        invitation.getAccountId(),
                        "current",
                        invitation.getPresentationEmail(),
                        invitation.getCanonicalEmail());
        assertThat(mapper.toAccessChallengeRecord(invitation, now, expiresAt))
                .extracting("id", "accountId", "purpose", "generation", "createdAt", "expiresAt")
                .containsExactly(
                        invitation.getInvitationId(),
                        invitation.getAccountId(),
                        "activation",
                        1,
                        now,
                        expiresAt);
        assertThat(mapper.toAdministratorDeclaration(invitation, now))
                .extracting("accountId", "actorAccountId", "actorKind", "origin", "textVersion")
                .containsExactly(
                        invitation.getAccountId(),
                        invitation.getAdministratorAccountId(),
                        "administrator",
                        "administrative_invitation",
                        "ux-02-v0.1");
    }

    private static PendingRunnerInvitation invitation() {
        return new PendingRunnerInvitation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "runner@example.invalid",
                "runner@example.invalid",
                UUID.randomUUID(),
                new byte[] {1},
                UUID.randomUUID());
    }
}
