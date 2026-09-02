package com.vgrunning.identityaccess.application.service;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.port.in.AuthenticateCredentialsUseCase;
import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import com.vgrunning.identityaccess.domain.account.valueobject.EmailAddress;
import java.text.Normalizer;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Autentica credenciales sin conocer HTTP, sesiones, cookies ni jOOQ. */
@RequiredArgsConstructor
public class AuthenticateCredentialsService implements AuthenticateCredentialsUseCase {
    private final AccountRepository accounts;
    private final PasswordHasher passwordHasher;

    /** Autentica sin revelar si el correo existe, está activo o tiene contraseña configurada. */
    @Override
    @Transactional
    public AuthenticateCredentialsUseCase.AuthenticatedAccount authenticate(
            String suppliedEmail, String suppliedPassword) {
        String canonicalEmail = EmailAddress.from(suppliedEmail).canonicalValue();
        String normalizedPassword = Normalizer.normalize(suppliedPassword, Normalizer.Form.NFC);

        Optional<AccountRepository.CredentialAccount> found =
                accounts.findCredentialAccount(canonicalEmail);
        boolean passwordMatches =
                passwordHasher.matchesForAuthentication(
                        normalizedPassword,
                        found.map(AccountRepository.CredentialAccount::passwordHash)
                                .filter(hash -> !hash.isBlank()));
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
                throw new InvalidCredentialsException();
            }
        }
        return new AuthenticateCredentialsUseCase.AuthenticatedAccount(
                account.id(), account.role(), account.status());
    }
}
