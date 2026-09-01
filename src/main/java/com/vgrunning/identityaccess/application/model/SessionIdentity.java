package com.vgrunning.identityaccess.application.model;

import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import java.util.UUID;

/** Identidad de una sesión resuelta por la aplicación, independiente de Spring Security. */
public record SessionIdentity(
        UUID sessionId, UUID accountId, AccountRole role, AccountStatus status) {
    public static SessionIdentity restore(
            UUID sessionId, UUID accountId, AccountRole role, AccountStatus status) {
        return new SessionIdentity(sessionId, accountId, role, status);
    }
}
