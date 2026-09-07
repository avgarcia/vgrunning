package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de credenciales y estado de cuenta. */
public interface AccountRepository {

    /**
     * Busca la credencial asociada a un correo ya canonicalizado.
     *
     * @param canonicalEmail correo normalizado para comparación
     * @return credencial y estado actuales cuando existe una cuenta asociada
     */
    Optional<CredentialAccount> findCredentialAccount(String canonicalEmail);

    /**
     * Sustituye el hash de una credencial cuando su versión continúa vigente.
     *
     * @param account credencial leída durante la autenticación
     * @param replacementHash hash Argon2id que reemplaza al actual
     * @return {@code true} si la actualización optimista se aplicó
     */
    boolean updatePasswordHash(CredentialAccount account, String replacementHash);

    /**
     * Proyección de persistencia mínima para verificar una credencial y rehasharla de forma
     * optimista.
     */
    record CredentialAccount(
            UUID id, AccountRole role, AccountStatus status, String passwordHash, long version) {}
}
