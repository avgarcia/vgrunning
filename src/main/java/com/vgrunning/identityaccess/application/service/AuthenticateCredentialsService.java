package com.vgrunning.identityaccess.application.service;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.mapper.AuthenticatedAccountMapper;
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
    private final AuthenticatedAccountMapper mapper;

    /** Autentica sin revelar si el correo existe, está activo o tiene contraseña configurada. */
    @Override
    @Transactional
    public AuthenticateCredentialsUseCase.AuthenticatedAccount authenticate(
            String suppliedEmail, String suppliedPassword) {
        String canonicalEmail = EmailAddress.from(suppliedEmail).canonicalValue();
        String normalizedPassword = Normalizer.normalize(suppliedPassword, Normalizer.Form.NFC);

        Optional<AccountRepository.CredentialAccount> found =
                accounts.findCredentialAccount(canonicalEmail);
        Optional<AccountRepository.CredentialAccount> authenticable =
                found.filter(account -> AccountStatus.ACTIVE.equals(account.status()))
                        .filter(account -> !account.passwordHash().isBlank());
        boolean passwordMatches =
                passwordHasher.matchesForAuthentication(
                        normalizedPassword,
                        authenticable.map(AccountRepository.CredentialAccount::passwordHash));
        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        AccountRepository.CredentialAccount account =
                authenticable.orElseThrow(InvalidCredentialsException::new);
        if (passwordHasher.needsRehash(account.passwordHash())) {
            boolean updated =
                    accounts.updatePasswordHash(account, passwordHasher.hash(normalizedPassword));
            if (!updated) {
                throw new InvalidCredentialsException();
            }
        }
        return mapper.toAuthenticatedAccount(account);
    }
}
