package com.vgrunning.identityaccess.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RunnerInvitationProvisioningRepositoryTest {
    @Test
    void keepsThePendingInvitationImmutable() {
        byte[] verifier = {1, 2};
        RunnerInvitationProvisioningRepository.PendingRunnerInvitation invitation =
                new RunnerInvitationProvisioningRepository.PendingRunnerInvitation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "runner@example.invalid",
                        "runner@example.invalid",
                        UUID.randomUUID(),
                        verifier,
                        UUID.randomUUID());

        verifier[0] = 9;
        byte[] copy = invitation.getSecretVerifier();
        copy[1] = 9;

        assertThat(invitation.getAccountId()).isNotNull();
        assertThat(invitation.getEmailId()).isNotNull();
        assertThat(invitation.getInvitationId()).isNotNull();
        assertThat(invitation.getDeclarationId()).isNotNull();
        assertThat(invitation.getPresentationEmail()).isEqualTo("runner@example.invalid");
        assertThat(invitation.getCanonicalEmail()).isEqualTo("runner@example.invalid");
        assertThat(invitation.getAdministratorAccountId()).isNotNull();
        assertThat(invitation.getSecretVerifier()).containsExactly(1, 2);
        assertThat(invitation.getCorrelationId()).isNotNull();
    }
}
