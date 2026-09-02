package com.vgrunning.identityaccess.application.usecase;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.model.SessionIdentity;
import com.vgrunning.identityaccess.application.model.SessionLogin;
import com.vgrunning.identityaccess.application.port.in.CreateSessionUseCase;
import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import com.vgrunning.identityaccess.domain.account.EmailAddress;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/** Autentica credenciales sin conocer HTTP, sesiones, cookies ni jOOQ. */
@RequiredArgsConstructor
public class SessionUseCaseHandler implements CreateSessionUseCase {
    private final AccountRepository accounts;
    private final PasswordHasher passwordHasher;

    @Override
    public SessionLogin create(String suppliedEmail, String suppliedPassword) {
        String canonicalEmail = EmailAddress.from(suppliedEmail).canonicalValue();
        String normalizedPassword = PasswordNormalizer.normalize(suppliedPassword);

        Optional<AccountRepository.CredentialAccount> found =
                accounts.findCredentialAccount(canonicalEmail);
        String hashToVerify =
                found.map(AccountRepository.CredentialAccount::passwordHash)
                        .filter(hash -> !hash.isBlank())
                        .orElseGet(passwordHasher::dummyHash);
        boolean passwordMatches = passwordHasher.matches(normalizedPassword, hashToVerify);
        boolean active =
                found.map(account -> AccountStatus.ACTIVE.equals(account.status())).orElse(false);
        if (!passwordMatches || !active) {
            throw new InvalidCredentialsException();
        }

        AccountRepository.CredentialAccount account = found.orElseThrow();
        if (passwordHasher.needsRehash(account.passwordHash())) {
            boolean updated =
                    accounts.updatePasswordHash(account, passwordHasher.hash(normalizedPassword));
            if (!updated) {
                throw new IllegalStateException(
                        "La cuenta cambió mientras se actualizaba su hash de contraseña.");
            }
        }
        SessionIdentity session =
                SessionIdentity.create(account.id(), account.role(), account.status());
        return SessionLogin.create(session);
    }
}
