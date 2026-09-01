package com.vgrunning.identityaccess.application.usecase;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.exception.RateLimitedException;
import com.vgrunning.identityaccess.application.model.SessionIdentity;
import com.vgrunning.identityaccess.application.model.SessionLogin;
import com.vgrunning.identityaccess.application.port.in.CreateSessionUseCase;
import com.vgrunning.identityaccess.application.port.in.ResolveSessionUseCase;
import com.vgrunning.identityaccess.application.port.in.RevokeSessionUseCase;
import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import com.vgrunning.identityaccess.application.port.out.DatabaseTransactionClock;
import com.vgrunning.identityaccess.application.port.out.LoginRateLimitRepository;
import com.vgrunning.identityaccess.application.port.out.LoginRateLimitRepository.FailureCounts;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.port.out.RateLimitKeyDeriver;
import com.vgrunning.identityaccess.application.port.out.SecurityEventRepository;
import com.vgrunning.identityaccess.application.port.out.SessionRepository;
import com.vgrunning.identityaccess.application.port.out.SessionTokenService;
import com.vgrunning.identityaccess.application.securityevent.SecurityEvent;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import com.vgrunning.identityaccess.domain.account.EmailAddress;
import com.vgrunning.identityaccess.domain.session.SessionSecurityPolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Orquesta el ciclo de vida de las sesiones sin conocer HTTP, cookies ni jOOQ. */
@RequiredArgsConstructor
public class SessionUseCaseHandler
        implements CreateSessionUseCase, ResolveSessionUseCase, RevokeSessionUseCase {
    private final AccountRepository accounts;
    private final SessionRepository sessions;
    private final LoginRateLimitRepository rateLimits;
    private final SecurityEventRepository events;
    private final DatabaseTransactionClock clock;
    private final RateLimitKeyDeriver rateLimitKeys;
    private final PasswordHasher passwordHasher;
    private final SessionTokenService sessionTokens;
    private final SessionSecurityPolicy policy;

    @Override
    public SessionLogin create(
            String suppliedEmail, String suppliedPassword, String remoteAddress) {
        String canonicalEmail = EmailAddress.from(suppliedEmail).canonicalValue();
        String normalizedPassword = PasswordNormalizer.normalize(suppliedPassword);
        OffsetDateTime now = clock.now();
        OffsetDateTime windowStart = windowStart(now, policy.rateWindow());
        OffsetDateTime windowEnd = windowStart.plus(policy.rateWindow());
        byte[] accountKey = rateLimitKeys.accountKey(canonicalEmail);
        byte[] ipKey = rateLimitKeys.ipKey(remoteAddress);
        FailureCounts current = rateLimits.currentFailures(accountKey, ipKey, windowStart);
        if (limitReached(current)) {
            FailureCounts incremented =
                    rateLimits.incrementFailures(accountKey, ipKey, windowStart, windowEnd, now);
            recordFirstLimitedAttempt(incremented, now);
            throw new RateLimitedException(Duration.between(now, windowEnd));
        }

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
            FailureCounts incremented =
                    rateLimits.incrementFailures(accountKey, ipKey, windowStart, windowEnd, now);
            if (limitExceeded(incremented)) {
                recordFirstLimitedAttempt(incremented, now);
                throw new RateLimitedException(Duration.between(now, windowEnd));
            }
            throw new InvalidCredentialsException();
        }

        AccountRepository.CredentialAccount account = found.orElseThrow();
        UUID correlationId = UUID.randomUUID();
        if (passwordHasher.needsRehash(account.passwordHash())) {
            boolean updated =
                    accounts.updatePasswordHash(
                            account, passwordHasher.hash(normalizedPassword), now);
            if (!updated) {
                throw new IllegalStateException(
                        "La cuenta cambió mientras se actualizaba su hash de contraseña.");
            }
            events.append(SecurityEvent.passwordRehashed(account.id(), now, correlationId));
        }

        SessionTokenService.GeneratedSessionToken token = sessionTokens.generate();
        SessionIdentity session =
                SessionIdentity.restore(
                        UUID.randomUUID(), account.id(), account.role(), account.status());
        sessions.create(session, token.verifier(), now, now.plus(policy.absoluteTimeout()));
        events.append(SecurityEvent.sessionCreated(session, now, correlationId));
        return SessionLogin.create(session, token.rawToken());
    }

    @Override
    public Optional<SessionIdentity> resolve(String rawSessionToken) {
        OffsetDateTime now = clock.now();
        Optional<SessionRepository.StoredSession> found =
                sessions.findByVerifierForUpdate(sessionTokens.verifier(rawSessionToken));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        SessionRepository.StoredSession stored = found.orElseThrow();
        SessionIdentity identity = stored.identity();
        if (stored.status() != AccountStatus.ACTIVE) {
            if (sessions.revoke(stored.id(), now, "account_inactive")) {
                events.append(
                        SecurityEvent.sessionRevokedBySystem(identity, now, UUID.randomUUID()));
            }
            return Optional.empty();
        }
        boolean expired =
                !now.isBefore(stored.absoluteExpiresAt())
                        || !now.isBefore(stored.lastUsedAt().plus(policy.idleTimeout()));
        if (expired) {
            if (sessions.revoke(stored.id(), now, "expired")) {
                events.append(SecurityEvent.sessionExpired(identity, now, UUID.randomUUID()));
            }
            return Optional.empty();
        }
        return sessions.touch(stored.id(), now) ? Optional.of(identity) : Optional.empty();
    }

    @Override
    public void revoke(SessionIdentity session) {
        OffsetDateTime now = clock.now();
        UUID correlationId = UUID.randomUUID();
        if (sessions.revoke(session.sessionId(), now, "logout")) {
            events.append(SecurityEvent.sessionRevoked(session, now, correlationId));
        }
    }

    private boolean limitReached(FailureCounts failures) {
        return failures.account() >= policy.accountFailureLimit()
                || failures.ip() >= policy.ipFailureLimit();
    }

    private boolean limitExceeded(FailureCounts failures) {
        return failures.account() > policy.accountFailureLimit()
                || failures.ip() > policy.ipFailureLimit();
    }

    private void recordFirstLimitedAttempt(FailureCounts failures, OffsetDateTime now) {
        if (failures.account() == policy.accountFailureLimit() + 1
                || failures.ip() == policy.ipFailureLimit() + 1) {
            events.append(SecurityEvent.loginRateLimited(now, UUID.randomUUID()));
        }
    }

    private static OffsetDateTime windowStart(OffsetDateTime now, Duration window) {
        long seconds = window.toSeconds();
        long epochSecond = now.toEpochSecond();
        return Instant.ofEpochSecond(epochSecond - Math.floorMod(epochSecond, seconds))
                .atOffset(ZoneOffset.UTC);
    }
}
