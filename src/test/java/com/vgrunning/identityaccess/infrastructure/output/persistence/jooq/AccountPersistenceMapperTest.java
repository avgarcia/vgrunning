package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.vgrunning.generated.jooq.identity_access.tables.records.AccountRecord;

class AccountPersistenceMapperTest {

    private final AccountPersistenceMapper mapper = new AccountPersistenceMapperImpl();

    @Test
    void mapsAStoredAccountToCredentialData() {
        AccountRecord record = new AccountRecord();
        UUID accountId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        record.setId(accountId);
        record.setRole("corredor");
        record.setStatus("active");
        record.setPasswordHash("hash");
        record.setVersion(3L);

        var credential = mapper.toCredentialAccount(record);

        assertThat(credential.id()).isEqualTo(accountId);
        assertThat(credential.role()).isEqualTo(AccountRole.CORREDOR);
        assertThat(credential.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(credential.passwordHash()).isEqualTo("hash");
        assertThat(credential.version()).isEqualTo(3L);
    }

    @Test
    void mapsSyntheticProvisionToInsertRecords() {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-03T10:00:00Z");
        var provision =
                new JooqSyntheticAccountRepository.SyntheticAccountProvision(
                        UUID.fromString("10000000-0000-0000-0000-000000000001"),
                        AccountRole.CORREDOR,
                        "runner@running-coach.invalid",
                        "runner@running-coach.invalid",
                        "hash",
                        now);

        AccountRecord account = mapper.toAccountRecord(provision);
        var email = mapper.toAccountEmailRecord(provision, UUID.randomUUID());

        assertThat(account.getRole()).isEqualTo("corredor");
        assertThat(account.getStatus()).isEqualTo("active");
        assertThat(account.getCreatedAt()).isEqualTo(now);
        assertThat(email.getUsage()).isEqualTo("current");
        assertThat(email.getConfirmedAt()).isEqualTo(now);
        assertThat(email.getExpiresAt()).isNull();
        assertThat(email.getReleasedAt()).isNull();
    }
}
