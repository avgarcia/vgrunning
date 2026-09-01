package com.vgrunning.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.application.port.out.IdentityAccessRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.port.out.RateLimitKeyDeriver;
import com.vgrunning.identityaccess.application.port.out.SessionTokenService;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import com.vgrunning.identityaccess.domain.session.SessionSecurityPolicy;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionServiceTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-31T08:00:00Z");
    private static final SessionSecurityPolicy POLICY =
            new SessionSecurityPolicy(
                    Duration.ofHours(12), Duration.ofDays(7), Duration.ofMinutes(15), 5, 20);
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private RepositoryStub repository;
    private PasswordHasherStub passwords;
    private TokenServiceStub tokens;
    private SessionService service;

    @BeforeEach
    void setUp() {
        repository = new RepositoryStub();
        passwords = new PasswordHasherStub();
        tokens = new TokenServiceStub();
        service =
                new SessionService(
                        repository, () -> NOW, new FixedRateLimitKeys(), passwords, tokens, POLICY);
    }

    @Test
    void rejectsAnAlreadyLimitedLoginBeforeLookingUpCredentials() {
        repository.limitedUntil = Optional.of(NOW.plusSeconds(41));

        assertThatThrownBy(() -> service.login("RUNNER@example.invalid", "password", "127.0.0.1"))
                .isInstanceOfSatisfying(
                        RateLimitedException.class,
                        exception -> {
                            assertThat(exception.code()).isEqualTo("rate_limit_exceeded");
                            assertThat(exception.getMessage())
                                    .isEqualTo("No se puede intentar el acceso en este momento");
                            assertThat(exception.retryAfter()).isEqualTo(Duration.ofSeconds(41));
                        });
        assertThat(repository.lookedUpEmail).isNull();
    }

    @Test
    void usesTheDummyHashAndReturnsTheGenericFailureForAnUnknownAccount() {
        passwords.matches = false;

        assertThatThrownBy(() -> service.login(" RUNNER@example.invalid ", "pass", "127.0.0.1"))
                .isInstanceOfSatisfying(
                        InvalidCredentialsException.class,
                        exception -> {
                            assertThat(exception.code()).isEqualTo("session_creation_rejected");
                            assertThat(exception.getMessage())
                                    .isEqualTo("No se ha podido iniciar sesión");
                        });
        assertThat(repository.lookedUpEmail).isEqualTo("runner@example.invalid");
        assertThat(passwords.verifiedHash).isEqualTo("dummy-hash");
        assertThat(repository.failedLoginRecorded).isTrue();
    }

    @Test
    void returnsTheSameFailureForAnInactiveAccountEvenWithAMatchingPassword() {
        repository.account = Optional.of(account(AccountStatus.DISABLED, "stored-hash"));
        passwords.matches = true;

        assertThatThrownBy(() -> service.login("runner@example.invalid", "pass", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(passwords.verifiedHash).isEqualTo("stored-hash");
    }

    @Test
    void appliesTheLimitRaisedByTheFailureThatExhaustsTheWindow() {
        repository.account = Optional.of(account(AccountStatus.ACTIVE, "stored-hash"));
        repository.failedLimitedUntil = Optional.of(NOW.plusMinutes(15));
        passwords.matches = false;

        assertThatThrownBy(() -> service.login("runner@example.invalid", "pass", "127.0.0.1"))
                .isInstanceOfSatisfying(
                        RateLimitedException.class,
                        exception ->
                                assertThat(exception.retryAfter())
                                        .isEqualTo(Duration.ofMinutes(15)));
    }

    @Test
    void createsAnOpaqueSessionWithoutRehashWhenTheHashIsCurrent() {
        repository.account = Optional.of(account(AccountStatus.ACTIVE, "stored-hash"));
        passwords.matches = true;
        passwords.needsRehash = false;

        SessionLogin login = service.login("runner@example.invalid", "pa\u0301ss", "127.0.0.1");

        assertThat(login.rawSessionToken()).isEqualTo("raw-token");
        assertThat(login.session()).isEqualTo(repository.session);
        assertThat(passwords.verifiedPassword).isEqualTo("páss");
        assertThat(repository.createdVerifier).containsExactly(7, 8, 9);
        assertThat(repository.createdAt).isEqualTo(NOW);
        assertThat(repository.absoluteExpiresAt).isEqualTo(NOW.plusDays(7));
        assertThat(repository.rehashedPassword).isNull();
    }

    @Test
    void rehashesAnOutdatedPasswordBeforeCreatingTheSession() {
        repository.account = Optional.of(account(AccountStatus.ACTIVE, "old-hash"));
        passwords.matches = true;
        passwords.needsRehash = true;

        service.login("runner@example.invalid", "password", "127.0.0.1");

        assertThat(repository.rehashedPassword).isEqualTo("new-hash:password");
        assertThat(repository.rehashCorrelationId).isNotNull();
        assertThat(repository.createdCorrelationId).isEqualTo(repository.rehashCorrelationId);
    }

    @Test
    void authenticatesAndRevokesUsingTheConfiguredSessionPolicy() {
        Optional<SessionIdentity> authenticated = service.authenticate("raw-session");

        assertThat(authenticated).contains(repository.session);
        assertThat(tokens.verifiedRawToken).isEqualTo("raw-session");
        assertThat(repository.authenticationIdleTimeout).isEqualTo(Duration.ofHours(12));

        service.logout(repository.session);

        assertThat(repository.revokedSession).isEqualTo(repository.session);
        assertThat(repository.revokedAt).isEqualTo(NOW);
        assertThat(repository.revocationReason).isEqualTo("logout");
        assertThat(repository.revocationCorrelationId).isNotNull();
    }

    private static IdentityAccessRepository.CredentialAccount account(
            AccountStatus status, String passwordHash) {
        return new IdentityAccessRepository.CredentialAccount(
                ACCOUNT_ID, AccountRole.CORREDOR, status, passwordHash, 3L);
    }

    private static final class FixedRateLimitKeys implements RateLimitKeyDeriver {
        @Override
        public byte[] accountKey(String canonicalEmail) {
            return new byte[] {1};
        }

        @Override
        public byte[] ipKey(String remoteAddress) {
            return new byte[] {2};
        }
    }

    private static final class PasswordHasherStub implements PasswordHasher {
        private boolean matches;
        private boolean needsRehash;
        private String verifiedPassword;
        private String verifiedHash;

        @Override
        public boolean matches(String password, String encodedPassword) {
            verifiedPassword = password;
            verifiedHash = encodedPassword;
            return matches;
        }

        @Override
        public String hash(String password) {
            return "new-hash:" + password;
        }

        @Override
        public boolean needsRehash(String encodedPassword) {
            return needsRehash;
        }

        @Override
        public String dummyHash() {
            return "dummy-hash";
        }
    }

    private static final class TokenServiceStub implements SessionTokenService {
        private String verifiedRawToken;

        @Override
        public GeneratedSessionToken generate() {
            return new GeneratedSessionToken("raw-token", new byte[] {7, 8, 9});
        }

        @Override
        public byte[] verifier(String rawToken) {
            verifiedRawToken = rawToken;
            return new byte[] {4, 5, 6};
        }
    }

    private static final class RepositoryStub implements IdentityAccessRepository {
        private Optional<CredentialAccount> account = Optional.empty();
        private Optional<OffsetDateTime> limitedUntil = Optional.empty();
        private Optional<OffsetDateTime> failedLimitedUntil = Optional.empty();
        private final SessionIdentity session =
                new SessionIdentity(
                        SESSION_ID, ACCOUNT_ID, AccountRole.CORREDOR, AccountStatus.ACTIVE);
        private String lookedUpEmail;
        private boolean failedLoginRecorded;
        private String rehashedPassword;
        private UUID rehashCorrelationId;
        private byte[] createdVerifier;
        private OffsetDateTime createdAt;
        private OffsetDateTime absoluteExpiresAt;
        private UUID createdCorrelationId;
        private Duration authenticationIdleTimeout;
        private SessionIdentity revokedSession;
        private OffsetDateTime revokedAt;
        private String revocationReason;
        private UUID revocationCorrelationId;

        @Override
        public Optional<CredentialAccount> findCredentialAccount(String canonicalEmail) {
            lookedUpEmail = canonicalEmail;
            return account;
        }

        @Override
        public Optional<OffsetDateTime> limitedUntil(
                byte[] accountKey,
                byte[] ipKey,
                OffsetDateTime now,
                int accountLimit,
                int ipLimit) {
            assertThat(accountKey).containsExactly(1);
            assertThat(ipKey).containsExactly(2);
            assertThat(now).isEqualTo(NOW);
            assertThat(accountLimit).isEqualTo(5);
            assertThat(ipLimit).isEqualTo(20);
            return limitedUntil;
        }

        @Override
        public Optional<OffsetDateTime> recordFailedLogin(
                byte[] accountKey,
                byte[] ipKey,
                OffsetDateTime now,
                int accountLimit,
                int ipLimit,
                Duration window) {
            failedLoginRecorded = true;
            assertThat(window).isEqualTo(Duration.ofMinutes(15));
            return failedLimitedUntil;
        }

        @Override
        public void rehashPassword(
                CredentialAccount account,
                String replacementHash,
                OffsetDateTime now,
                UUID correlationId) {
            rehashedPassword = replacementHash;
            rehashCorrelationId = correlationId;
        }

        @Override
        public SessionIdentity createSession(
                CredentialAccount account,
                byte[] verifierHash,
                OffsetDateTime now,
                OffsetDateTime absoluteExpiresAt,
                UUID correlationId) {
            createdVerifier = verifierHash;
            createdAt = now;
            this.absoluteExpiresAt = absoluteExpiresAt;
            createdCorrelationId = correlationId;
            return session;
        }

        @Override
        public Optional<SessionIdentity> authenticate(
                byte[] verifierHash, OffsetDateTime now, Duration idleTimeout) {
            assertThat(verifierHash).containsExactly(4, 5, 6);
            assertThat(now).isEqualTo(NOW);
            authenticationIdleTimeout = idleTimeout;
            return Optional.of(session);
        }

        @Override
        public void revoke(
                SessionIdentity session, OffsetDateTime now, String reason, UUID correlationId) {
            revokedSession = session;
            revokedAt = now;
            revocationReason = reason;
            revocationCorrelationId = correlationId;
        }

        @Override
        public void provisionSyntheticAccount(
                UUID accountId,
                AccountRole role,
                String presentationEmail,
                String canonicalEmail,
                String passwordHash,
                OffsetDateTime now) {
            throw new UnsupportedOperationException();
        }
    }
}
