package com.vgrunning.identityaccess.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.application.port.out.AccountRepository.CredentialAccount;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AuthenticatedAccountMapperTest {

    private final AuthenticatedAccountMapper mapper =
            Mappers.getMapper(AuthenticatedAccountMapper.class);

    @Test
    void mapsOnlyTheAuthenticatedIdentity() {
        UUID accountId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        CredentialAccount credential =
                new CredentialAccount(
                        accountId, AccountRole.CORREDOR, AccountStatus.ACTIVE, "hash", 3L);

        var authenticated = mapper.toAuthenticatedAccount(credential);

        assertThat(authenticated.accountId()).isEqualTo(accountId);
        assertThat(authenticated.role()).isEqualTo(AccountRole.CORREDOR);
        assertThat(authenticated.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void mapsANullSourceToNull() {
        assertThat(mapper.toAuthenticatedAccount(null)).isNull();
    }
}
