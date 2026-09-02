package com.vgrunning.identityaccess.application.port.in;

import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.util.UUID;

/** Puerto de entrada para autenticar credenciales. */
public interface CreateSessionUseCase {
    AuthenticatedAccount create(String email, String password);

    record AuthenticatedAccount(UUID accountId, AccountRole role, AccountStatus status) {}
}
