package com.vgrunning.identityaccess.application;

import com.vgrunning.identityaccess.adapter.persistence.IdentityAccessRepository;
import com.vgrunning.identityaccess.adapter.persistence.PostgresTimeProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso de sesión opaca; no expone hashes ni datos de contacto a otros módulos. */
@Service
public class SessionService {
    static final Duration IDLE_TIMEOUT = Duration.ofHours(12);
    static final Duration ABSOLUTE_TIMEOUT = Duration.ofDays(7);
    private static final Duration RATE_WINDOW = Duration.ofMinutes(15);
    private static final int ACCOUNT_FAILURE_LIMIT = 5;
    private static final int IP_FAILURE_LIMIT = 20;
    private final IdentityAccessRepository repository;
    private final PostgresTimeProvider timeProvider;
    private final IdentityNormalizer normalizer;
    private final RateLimitKeyDeriver rateLimitKeys;
    private final Argon2PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionService(
            IdentityAccessRepository repository,
            PostgresTimeProvider timeProvider,
            IdentityNormalizer normalizer,
            RateLimitKeyDeriver rateLimitKeys,
            Argon2PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.timeProvider = timeProvider;
        this.normalizer = normalizer;
        this.rateLimitKeys = rateLimitKeys;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SessionLogin login(String suppliedEmail, String suppliedPassword, String remoteAddress) {
        String canonicalEmail = normalizer.canonicalEmail(suppliedEmail);
        String normalizedPassword = normalizer.normalizedPassword(suppliedPassword);
        OffsetDateTime now = timeProvider.now();
        byte[] accountKey = rateLimitKeys.accountKey(canonicalEmail);
        byte[] ipKey = rateLimitKeys.ipKey(remoteAddress);
        Optional<OffsetDateTime> alreadyLimitedUntil =
                repository.limitedUntil(
                        accountKey, ipKey, now, ACCOUNT_FAILURE_LIMIT, IP_FAILURE_LIMIT);
        if (alreadyLimitedUntil.isPresent()) {
            throw new RateLimitedException(
                    Duration.between(now, alreadyLimitedUntil.orElseThrow()));
        }

        Optional<IdentityAccessRepository.CredentialAccount> found =
                repository.findCredentialAccount(canonicalEmail);
        String hashToVerify =
                found.map(IdentityAccessRepository.CredentialAccount::passwordHash)
                        .filter(hash -> !hash.isBlank())
                        .orElseGet(repository::dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(normalizedPassword, hashToVerify);
        boolean active = found.map(account -> "active".equals(account.status())).orElse(false);
        if (!passwordMatches || !active) {
            Optional<OffsetDateTime> limitedUntil =
                    repository.recordFailedLogin(
                            accountKey,
                            ipKey,
                            now,
                            ACCOUNT_FAILURE_LIMIT,
                            IP_FAILURE_LIMIT,
                            RATE_WINDOW);
            if (limitedUntil.isPresent()) {
                throw new RateLimitedException(Duration.between(now, limitedUntil.orElseThrow()));
            }
            throw new InvalidCredentialsException();
        }

        IdentityAccessRepository.CredentialAccount account = found.orElseThrow();
        UUID correlationId = UUID.randomUUID();
        if (passwordEncoder.upgradeEncoding(account.passwordHash())) {
            repository.rehashPassword(
                    account, passwordEncoder.encode(normalizedPassword), now, correlationId);
        }
        String rawToken = generateRawToken();
        AuthenticatedSession session =
                repository.createSession(
                        account, sha256(rawToken), now, now.plus(ABSOLUTE_TIMEOUT), correlationId);
        return new SessionLogin(session, rawToken);
    }

    @Transactional
    public Optional<AuthenticatedSession> authenticate(String rawSessionToken) {
        OffsetDateTime now = timeProvider.now();
        return repository.authenticate(sha256(rawSessionToken), now, IDLE_TIMEOUT);
    }

    @Transactional
    public void logout(AuthenticatedSession session) {
        repository.revoke(session, timeProvider.now(), "logout", UUID.randomUUID());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] sha256(String rawValue) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(rawValue.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM.", exception);
        }
    }
}
