package com.vgrunning.identityaccess.infrastructure.security.session;

import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/** Identidad autenticada serializable que Spring Session guarda junto con la sesión HTTP. */
public record SessionPrincipal(UUID accountId, AccountRole role, AccountStatus status)
        implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /** Crea el principal mínimo que Spring Session serializa en la sesión HTTP. */
    public static SessionPrincipal create(UUID accountId, AccountRole role, AccountStatus status) {
        return new SessionPrincipal(accountId, role, status);
    }
}
