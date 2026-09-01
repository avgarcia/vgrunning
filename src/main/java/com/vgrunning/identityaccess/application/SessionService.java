package com.vgrunning.identityaccess.application;

import com.vgrunning.identityaccess.application.port.out.CurrentTimeProvider;
import com.vgrunning.identityaccess.application.port.out.IdentityAccessRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.port.out.RateLimitKeyDeriver;
import com.vgrunning.identityaccess.application.port.out.SessionTokenService;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import com.vgrunning.identityaccess.domain.account.EmailAddress;
import com.vgrunning.identityaccess.domain.session.SessionSecurityPolicy;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso de sesión opaca; no expone hashes ni datos de contacto a otros módulos. */
public class SessionService {
    private final IdentityAccessRepository repository;
    private final CurrentTimeProvider timeProvider;
    private final RateLimitKeyDeriver rateLimitKeys;
    private final PasswordHasher passwordHasher;
    private final SessionTokenService sessionTokens;
    private final SessionSecurityPolicy policy;

    public SessionService(
            IdentityAccessRepository repository,
            CurrentTimeProvider timeProvider,
            RateLimitKeyDeriver rateLimitKeys,
            PasswordHasher passwordHasher,
            SessionTokenService sessionTokens,
            SessionSecurityPolicy policy) {
        this.repository = repository;
        this.timeProvider = timeProvider;
        this.rateLimitKeys = rateLimitKeys;
        this.passwordHasher = passwordHasher;
        this.sessionTokens = sessionTokens;
        this.policy = policy;
    }

    @Transactional
    public SessionLogin login(String suppliedEmail, String suppliedPassword, String remoteAddress) {
        String canonicalEmail = EmailAddress.from(suppliedEmail).canonicalValue();
        String normalizedPassword = PasswordNormalizer.normalize(suppliedPassword);
        OffsetDateTime now = timeProvider.now();
        byte[] accountKey = rateLimitKeys.accountKey(canonicalEmail);
        byte[] ipKey = rateLimitKeys.ipKey(remoteAddress);
        Optional<OffsetDateTime> alreadyLimitedUntil =
                repository.limitedUntil(
                        accountKey,
                        ipKey,
                        now,
                        policy.accountFailureLimit(),
                        policy.ipFailureLimit());
        if (alreadyLimitedUntil.isPresent()) {
            throw new RateLimitedException(
                    Duration.between(now, alreadyLimitedUntil.orElseThrow()));
        }

        Optional<IdentityAccessRepository.CredentialAccount> found =
                repository.findCredentialAccount(canonicalEmail);
        String hashToVerify =
                found.map(IdentityAccessRepository.CredentialAccount::passwordHash)
                        .filter(hash -> !hash.isBlank())
                        .orElseGet(passwordHasher::dummyHash);
        boolean passwordMatches = passwordHasher.matches(normalizedPassword, hashToVerify);
        boolean active =
                found.map(account -> AccountStatus.ACTIVE.equals(account.status())).orElse(false);
        if (!passwordMatches || !active) {
            Optional<OffsetDateTime> limitedUntil =
                    repository.recordFailedLogin(
                            accountKey,
                            ipKey,
                            now,
                            policy.accountFailureLimit(),
                            policy.ipFailureLimit(),
                            policy.rateWindow());
            if (limitedUntil.isPresent()) {
                throw new RateLimitedException(Duration.between(now, limitedUntil.orElseThrow()));
            }
            throw new InvalidCredentialsException();
        }

        IdentityAccessRepository.CredentialAccount account = found.orElseThrow();
        UUID correlationId = UUID.randomUUID();
        if (passwordHasher.needsRehash(account.passwordHash())) {
            repository.rehashPassword(
                    account, passwordHasher.hash(normalizedPassword), now, correlationId);
        }
        SessionTokenService.GeneratedSessionToken token = sessionTokens.generate();
        SessionIdentity session =
                repository.createSession(
                        account,
                        token.verifier(),
                        now,
                        now.plus(policy.absoluteTimeout()),
                        correlationId);
        return new SessionLogin(session, token.rawToken());
    }

    @Transactional
    public Optional<SessionIdentity> authenticate(String rawSessionToken) {
        OffsetDateTime now = timeProvider.now();
        return repository.authenticate(
                sessionTokens.verifier(rawSessionToken), now, policy.idleTimeout());
    }

    @Transactional
    public void logout(SessionIdentity session) {
        repository.revoke(session, timeProvider.now(), "logout", UUID.randomUUID());
    }
}
