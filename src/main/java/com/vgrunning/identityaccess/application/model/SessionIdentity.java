package com.vgrunning.identityaccess.application.model;

import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/** Identidad autenticada serializable que Spring Session guarda junto con la sesión HTTP. */
public record SessionIdentity(UUID accountId, AccountRole role, AccountStatus status)
        implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public static SessionIdentity create(UUID accountId, AccountRole role, AccountStatus status) {
        return new SessionIdentity(accountId, role, status);
    }
}
