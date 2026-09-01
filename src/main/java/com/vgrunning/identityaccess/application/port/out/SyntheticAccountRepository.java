package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.domain.account.AccountRole;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Persistencia exclusiva del bootstrap sintético de infraestructura. */
public interface SyntheticAccountRepository {
    boolean provision(
            UUID accountId,
            AccountRole role,
            String presentationEmail,
            String canonicalEmail,
            String passwordHash,
            OffsetDateTime now);
}
