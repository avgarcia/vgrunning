package com.vgrunning.identityaccess.infrastructure.input.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.application.port.in.AuthenticateCredentialsUseCase.AuthenticatedAccount;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import com.vgrunning.identityaccess.infrastructure.security.session.SessionPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.vgrunning.generated.openapi.server.model.CurrentSession;

/** Comprueba todos los campos expuestos por el mapeo HTTP de la sesión actual. */
class SessionWebMapperTest {
    private final SessionWebMapper mapper = Mappers.getMapper(SessionWebMapper.class);

    @Test
    void mapsEveryCurrentSessionField() {
        UUID accountId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        SessionPrincipal principal =
                new SessionPrincipal(accountId, AccountRole.CORREDOR, AccountStatus.ACTIVE);

        CurrentSession response = mapper.toResponse(principal);

        assertThat(response.getAccountId()).isEqualTo(accountId);
        assertThat(response.getRole().getValue()).isEqualTo("corredor");
        assertThat(response.getAccountStatus().getValue()).isEqualTo("active");
    }

    @Test
    void mapsAuthenticatedAccountToTheSessionPrincipal() {
        UUID accountId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        SessionPrincipal principal =
                mapper.toPrincipal(
                        new AuthenticatedAccount(
                                accountId, AccountRole.CORREDOR, AccountStatus.ACTIVE));

        assertThat(principal.accountId()).isEqualTo(accountId);
        assertThat(principal.role()).isEqualTo(AccountRole.CORREDOR);
        assertThat(principal.status()).isEqualTo(AccountStatus.ACTIVE);
    }
}
