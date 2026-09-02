package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de credenciales y estado de cuenta. */
public interface AccountRepository {
    Optional<CredentialAccount> findCredentialAccount(String canonicalEmail);

    boolean updatePasswordHash(CredentialAccount account, String replacementHash);

    record CredentialAccount(
            UUID id, AccountRole role, AccountStatus status, String passwordHash, long version) {
        public static CredentialAccount restore(
                UUID id,
                AccountRole role,
                AccountStatus status,
                String passwordHash,
                long version) {
            return new CredentialAccount(id, role, status, passwordHash, version);
        }
    }
}
