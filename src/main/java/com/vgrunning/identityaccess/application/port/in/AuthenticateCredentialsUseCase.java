package com.vgrunning.identityaccess.application.port.in;

import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.util.UUID;

/** Puerto de entrada que autentica credenciales sin crear una sesión HTTP. */
public interface AuthenticateCredentialsUseCase {

    /**
     * Autentica las credenciales presentadas y devuelve la identidad vigente de la cuenta.
     *
     * @param email correo presentado por la persona usuaria
     * @param password contraseña presentada por la persona usuaria
     * @return identidad autenticada que la infraestructura podrá asociar a una sesión HTTP
     */
    AuthenticatedAccount authenticate(String email, String password);

    /** Resultado exclusivo del contrato de autenticación; no representa una entidad de dominio. */
    record AuthenticatedAccount(UUID accountId, AccountRole role, AccountStatus status) {}
}
