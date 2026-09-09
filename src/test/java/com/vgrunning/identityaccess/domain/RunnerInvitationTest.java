package com.vgrunning.identityaccess.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.domain.account.valueobject.EmailAddress;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RunnerInvitationTest {

    @Test
    void createGeneratesFourDistinctIdentifiers() {
        RunnerInvitation invitation =
                RunnerInvitation.create(
                        EmailAddress.from("Runner@Example.invalid"),
                        "Runner@Example.invalid",
                        UUID.randomUUID(),
                        new byte[] {1, 2},
                        UUID.randomUUID());

        assertThat(invitation.presentationEmail()).isEqualTo("Runner@Example.invalid");
        assertThat(invitation.canonicalEmail()).isEqualTo("runner@example.invalid");
        assertThat(
                        java.util.Set.of(
                                invitation.accountId(),
                                invitation.emailId(),
                                invitation.invitationId(),
                                invitation.declarationId()))
                .hasSize(4);
    }

    @Test
    void clonesTheSecretVerifierOnConstructionAndOnRead() {
        byte[] verifier = {1, 2};
        RunnerInvitation invitation =
                new RunnerInvitation(
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
        invitation.secretVerifier()[1] = 9;

        assertThat(invitation.secretVerifier()).containsExactly(1, 2);
    }

    @Test
    void equalsAndHashCodeCompareSecretVerifierContentNotReference() {
        UUID accountId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        UUID declarationId = UUID.randomUUID();
        UUID administratorId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        RunnerInvitation first =
                new RunnerInvitation(
                        accountId,
                        emailId,
                        invitationId,
                        declarationId,
                        "runner@example.invalid",
                        "runner@example.invalid",
                        administratorId,
                        new byte[] {1, 2},
                        correlationId);
        RunnerInvitation same =
                new RunnerInvitation(
                        accountId,
                        emailId,
                        invitationId,
                        declarationId,
                        "runner@example.invalid",
                        "runner@example.invalid",
                        administratorId,
                        new byte[] {1, 2},
                        correlationId);
        RunnerInvitation differentVerifier =
                new RunnerInvitation(
                        accountId,
                        emailId,
                        invitationId,
                        declarationId,
                        "runner@example.invalid",
                        "runner@example.invalid",
                        administratorId,
                        new byte[] {9, 9},
                        correlationId);
        RunnerInvitation differentAccountId =
                new RunnerInvitation(
                        UUID.randomUUID(),
                        emailId,
                        invitationId,
                        declarationId,
                        "runner@example.invalid",
                        "runner@example.invalid",
                        administratorId,
                        new byte[] {1, 2},
                        correlationId);
        RunnerInvitation differentEmailId =
                new RunnerInvitation(
                        accountId,
                        UUID.randomUUID(),
                        invitationId,
                        declarationId,
                        "runner@example.invalid",
                        "runner@example.invalid",
                        administratorId,
                        new byte[] {1, 2},
                        correlationId);
        RunnerInvitation differentInvitationId =
                new RunnerInvitation(
                        accountId,
                        emailId,
                        UUID.randomUUID(),
                        declarationId,
                        "runner@example.invalid",
                        "runner@example.invalid",
                        administratorId,
                        new byte[] {1, 2},
                        correlationId);
        RunnerInvitation differentDeclarationId =
                new RunnerInvitation(
                        accountId,
                        emailId,
                        invitationId,
                        UUID.randomUUID(),
                        "runner@example.invalid",
                        "runner@example.invalid",
                        administratorId,
                        new byte[] {1, 2},
                        correlationId);
        RunnerInvitation differentPresentationEmail =
                new RunnerInvitation(
                        accountId,
                        emailId,
                        invitationId,
                        declarationId,
                        "other@example.invalid",
                        "runner@example.invalid",
                        administratorId,
                        new byte[] {1, 2},
                        correlationId);
        RunnerInvitation differentCanonicalEmail =
                new RunnerInvitation(
                        accountId,
                        emailId,
                        invitationId,
                        declarationId,
                        "runner@example.invalid",
                        "other@example.invalid",
                        administratorId,
                        new byte[] {1, 2},
                        correlationId);
        RunnerInvitation differentAdministratorId =
                new RunnerInvitation(
                        accountId,
                        emailId,
                        invitationId,
                        declarationId,
                        "runner@example.invalid",
                        "runner@example.invalid",
                        UUID.randomUUID(),
                        new byte[] {1, 2},
                        correlationId);
        RunnerInvitation differentCorrelationId =
                new RunnerInvitation(
                        accountId,
                        emailId,
                        invitationId,
                        declarationId,
                        "runner@example.invalid",
                        "runner@example.invalid",
                        administratorId,
                        new byte[] {1, 2},
                        UUID.randomUUID());

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(first).isNotEqualTo(differentVerifier);
        assertThat(first).isNotEqualTo(differentAccountId);
        assertThat(first).isNotEqualTo(differentEmailId);
        assertThat(first).isNotEqualTo(differentInvitationId);
        assertThat(first).isNotEqualTo(differentDeclarationId);
        assertThat(first).isNotEqualTo(differentPresentationEmail);
        assertThat(first).isNotEqualTo(differentCanonicalEmail);
        assertThat(first).isNotEqualTo(differentAdministratorId);
        assertThat(first).isNotEqualTo(differentCorrelationId);
        assertThat(first).isNotEqualTo("not-a-runner-invitation");
    }

    @Test
    void rejectsMissingFields() {
        UUID id = UUID.randomUUID();
        byte[] verifier = {1};
        assertThatThrownBy(
                        () ->
                                new RunnerInvitation(
                                        null,
                                        id,
                                        id,
                                        id,
                                        "a@b.invalid",
                                        "a@b.invalid",
                                        id,
                                        verifier,
                                        id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                new RunnerInvitation(
                                        id, id, id, id, "a@b.invalid", "a@b.invalid", id, null, id))
                .isInstanceOf(NullPointerException.class);
    }
}
