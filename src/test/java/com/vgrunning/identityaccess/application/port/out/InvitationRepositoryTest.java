package com.vgrunning.identityaccess.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.application.port.out.InvitationRepository.ActivationInvitation;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvitationRepositoryTest {
    private static final UUID ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

    @Test
    void clonesTheVerifierOnConstructionAndOnRead() {
        byte[] verifier = {1, 2, 3};
        ActivationInvitation invitation =
                new ActivationInvitation(ID, ACCOUNT_ID, verifier, EXPIRES_AT);

        verifier[0] = 9;
        invitation.verifier()[0] = 9;

        assertThat(invitation.verifier()).containsExactly(1, 2, 3);
    }

    @Test
    void equalsAndHashCodeCompareVerifierContentNotReference() {
        ActivationInvitation first =
                new ActivationInvitation(ID, ACCOUNT_ID, new byte[] {1, 2, 3}, EXPIRES_AT);
        ActivationInvitation same =
                new ActivationInvitation(ID, ACCOUNT_ID, new byte[] {1, 2, 3}, EXPIRES_AT);
        ActivationInvitation differentVerifier =
                new ActivationInvitation(ID, ACCOUNT_ID, new byte[] {9, 9, 9}, EXPIRES_AT);
        ActivationInvitation differentId =
                new ActivationInvitation(
                        UUID.randomUUID(), ACCOUNT_ID, new byte[] {1, 2, 3}, EXPIRES_AT);

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(first).isNotEqualTo(differentVerifier);
        assertThat(first).isNotEqualTo(differentId);
        assertThat(first).isNotEqualTo("not-an-invitation");
    }

    @Test
    void rejectsMissingFields() {
        byte[] verifier = {1};
        assertThatThrownBy(() -> new ActivationInvitation(null, ACCOUNT_ID, verifier, EXPIRES_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ActivationInvitation(ID, null, verifier, EXPIRES_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ActivationInvitation(ID, ACCOUNT_ID, null, EXPIRES_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ActivationInvitation(ID, ACCOUNT_ID, verifier, null))
                .isInstanceOf(NullPointerException.class);
    }
}
