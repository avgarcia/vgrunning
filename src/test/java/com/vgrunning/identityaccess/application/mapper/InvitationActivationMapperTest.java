package com.vgrunning.identityaccess.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;
import com.vgrunning.identityaccess.application.port.out.InvitationRepository.ActivationInvitation;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class InvitationActivationMapperTest {
    private final InvitationActivationMapper mapper =
            Mappers.getMapper(InvitationActivationMapper.class);

    @Test
    void mapsTheAcceptedInvitationAndHandlesAbsentSources() {
        UUID accountId = UUID.fromString("20000000-0000-0000-0000-000000000002");
        UUID correlationId = UUID.fromString("20000000-0000-0000-0000-000000000003");
        ActivationInvitation invitation =
                new ActivationInvitation(
                        UUID.fromString("20000000-0000-0000-0000-000000000001"),
                        accountId,
                        new byte[] {1},
                        OffsetDateTime.now(ZoneOffset.UTC));

        assertThat(mapper.toActivation(invitation, correlationId))
                .extracting("accountId", "correlationId")
                .containsExactly(accountId, correlationId);
        assertThat(invitation.id()).isNotNull();
        assertThat(invitation.accountId()).isEqualTo(accountId);
        assertThat(invitation.expiresAt()).isNotNull();
        assertThat(mapper.toActivation(null, correlationId))
                .isEqualTo(new RunnerInvitationActivated(null, correlationId));
        assertThat(mapper.toActivation(null, null)).isNull();
    }
}
