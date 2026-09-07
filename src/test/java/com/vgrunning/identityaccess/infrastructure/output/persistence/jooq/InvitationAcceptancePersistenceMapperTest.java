package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.vgrunning.generated.jooq.identity_access.tables.records.AccessChallengeRecord;

class InvitationAcceptancePersistenceMapperTest {
    private final InvitationAcceptancePersistenceMapper mapper =
            new InvitationAcceptancePersistenceMapperImpl();

    @Test
    void mapsTheLockedChallengeToTheActivationContract() {
        AccessChallengeRecord challenge = new AccessChallengeRecord();
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.parse("2026-09-08T10:00:00Z");
        challenge.setId(id);
        challenge.setAccountId(accountId);
        challenge.setVerifierSha256(new byte[] {1, 2});
        challenge.setExpiresAt(expiresAt);

        assertThat(mapper.toActivationInvitation(challenge))
                .extracting("id", "accountId", "expiresAt")
                .containsExactly(id, accountId, expiresAt);
        assertThat(mapper.toActivationInvitation(null)).isNull();
    }
}
