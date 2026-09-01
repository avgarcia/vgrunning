package com.vgrunning.identityaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.exception.RateLimitedException;
import com.vgrunning.identityaccess.application.model.SessionIdentity;
import com.vgrunning.identityaccess.application.model.SessionLogin;
import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import com.vgrunning.identityaccess.application.port.out.LoginRateLimitRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.port.out.RateLimitKeyDeriver;
import com.vgrunning.identityaccess.application.port.out.SecurityEventRepository;
import com.vgrunning.identityaccess.application.port.out.SessionRepository;
import com.vgrunning.identityaccess.application.port.out.SessionTokenService;
import com.vgrunning.identityaccess.application.securityevent.SecurityEvent;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import com.vgrunning.identityaccess.domain.session.SessionSecurityPolicy;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prueba unitaria del caso de uso: no arranca Spring ni accede a PostgreSQL. */
class SessionUseCaseHandlerTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-31T08:00:00Z");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final SessionSecurityPolicy POLICY =
            new SessionSecurityPolicy(
                    Duration.ofHours(12), Duration.ofDays(7), Duration.ofMinutes(15), 5, 20);

    private StoreFake store;
    private PasswordsFake passwords;
    private SessionUseCaseHandler useCase;

    @BeforeEach
    void setUp() {
        store = new StoreFake();
        passwords = new PasswordsFake();
        useCase =
                new SessionUseCaseHandler(
                        store,
                        store,
                        store,
                        store,
                        () -> NOW,
                        new FixedRateLimitKeys(),
                        passwords,
                        new TokensFake(),
                        POLICY);
    }

    @Test
    void createsTheOpaqueSessionAndItsEvents() {
        store.account = Optional.of(account(AccountStatus.ACTIVE));
        passwords.matches = true;
        passwords.needsRehash = true;

        SessionLogin login = useCase.create(" RUNNER@example.invalid ", "pa\u0301ss", "127.0.0.1");

        assertThat(login.rawSessionToken()).isEqualTo("raw-token");
        assertThat(login.session().accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(store.canonicalEmail).isEqualTo("runner@example.invalid");
        assertThat(passwords.verifiedPassword).isEqualTo("páss");
        assertThat(store.replacementHash).isEqualTo("new-hash:páss");
        assertThat(store.createdVerifier).containsExactly(7, 8, 9);
        assertThat(store.absoluteExpiresAt).isEqualTo(NOW.plusDays(7));
        assertThat(store.events)
                .extracting(SecurityEvent::type)
                .containsExactly(
                        SecurityEvent.Type.PASSWORD_REHASHED, SecurityEvent.Type.SESSION_CREATED);
    }

    @Test
    void returnsTheSameFailureForUnknownAndInactiveAccounts() {
        passwords.matches = false;
        assertThatThrownBy(() -> useCase.create("runner@example.invalid", "password", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(passwords.verifiedHash).isEqualTo("dummy-hash");

        store.account = Optional.of(account(AccountStatus.DISABLED));
        passwords.matches = true;
        assertThatThrownBy(() -> useCase.create("runner@example.invalid", "password", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsAndAuditsTheFirstAttemptBeyondTheFixedWindowLimit() {
        store.currentFailures = new LoginRateLimitRepository.FailureCounts(5, 2);
        store.incrementedFailures = new LoginRateLimitRepository.FailureCounts(6, 3);

        assertThatThrownBy(() -> useCase.create("runner@example.invalid", "password", "127.0.0.1"))
                .isInstanceOfSatisfying(
                        RateLimitedException.class,
                        exception ->
                                assertThat(exception.retryAfter())
                                        .isEqualTo(Duration.ofMinutes(15)));
        assertThat(store.events)
                .extracting(SecurityEvent::type)
                .containsExactly(SecurityEvent.Type.LOGIN_RATE_LIMITED);
    }

    @Test
    void appliesTheIpLimitAndDoesNotDuplicateItsEvent() {
        store.currentFailures = new LoginRateLimitRepository.FailureCounts(0, 20);
        store.incrementedFailures = new LoginRateLimitRepository.FailureCounts(1, 22);

        assertThatThrownBy(() -> useCase.create("runner@example.invalid", "password", "127.0.0.1"))
                .isInstanceOf(RateLimitedException.class);
        assertThat(store.events).isEmpty();
    }

    @Test
    void limitsTheFailedAttemptThatExceedsTheIpLimit() {
        passwords.matches = false;
        store.incrementedFailures = new LoginRateLimitRepository.FailureCounts(1, 21);

        assertThatThrownBy(() -> useCase.create("runner@example.invalid", "password", "127.0.0.1"))
                .isInstanceOf(RateLimitedException.class);
        assertThat(store.events)
                .extracting(SecurityEvent::type)
                .containsExactly(SecurityEvent.Type.LOGIN_RATE_LIMITED);
    }

    @Test
    void failsWhenTheOptimisticPasswordUpdateLosesTheRace() {
        store.account = Optional.of(account(AccountStatus.ACTIVE));
        store.passwordUpdated = false;
        passwords.matches = true;
        passwords.needsRehash = true;

        assertThatThrownBy(() -> useCase.create("runner@example.invalid", "password", "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cuenta cambió");
        assertThat(store.events).isEmpty();
    }

    @Test
    void resolvesAnActiveSessionAndTouchesItsLastUse() {
        store.storedSession =
                Optional.of(stored(NOW.minusHours(1), NOW.plusDays(1), AccountStatus.ACTIVE));

        assertThat(useCase.resolve("raw-session"))
                .contains(store.storedSession.orElseThrow().identity());
        assertThat(store.touchedAt).isEqualTo(NOW);
    }

    @Test
    void expiresAnIdleSessionOnce() {
        store.storedSession =
                Optional.of(stored(NOW.minusHours(13), NOW.plusDays(1), AccountStatus.ACTIVE));

        assertThat(useCase.resolve("raw-session")).isEmpty();
        assertThat(store.revocationReason).isEqualTo("expired");
        assertThat(store.events)
                .extracting(SecurityEvent::type)
                .containsExactly(SecurityEvent.Type.SESSION_EXPIRED);
    }

    @Test
    void revokesAnInactiveAccountAsASystemAction() {
        store.storedSession =
                Optional.of(stored(NOW.minusHours(1), NOW.plusDays(1), AccountStatus.DISABLED));

        assertThat(useCase.resolve("raw-session")).isEmpty();
        assertThat(store.revocationReason).isEqualTo("account_inactive");
        assertThat(store.events)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.type()).isEqualTo(SecurityEvent.Type.SESSION_REVOKED);
                            assertThat(event.actorClass())
                                    .isEqualTo(SecurityEvent.ActorClass.SYSTEM);
                        });
    }

    @Test
    void returnsEmptyForUnknownSessionsAndForAConcurrentTouch() {
        assertThat(useCase.resolve("missing")).isEmpty();

        store.storedSession =
                Optional.of(stored(NOW.minusHours(1), NOW.plusDays(1), AccountStatus.ACTIVE));
        store.sessionTouched = false;
        assertThat(useCase.resolve("concurrent")).isEmpty();
    }

    @Test
    void doesNotDuplicateAnExpirationEventWhenRevocationLosesTheRace() {
        store.storedSession = Optional.of(stored(NOW.minusMinutes(1), NOW, AccountStatus.ACTIVE));
        store.sessionRevoked = false;

        assertThat(useCase.resolve("raw-session")).isEmpty();
        assertThat(store.events).isEmpty();
    }

    @Test
    void revokesAResolvedSessionOnLogout() {
        SessionIdentity session =
                SessionIdentity.restore(
                        SESSION_ID, ACCOUNT_ID, AccountRole.CORREDOR, AccountStatus.ACTIVE);

        useCase.revoke(session);

        assertThat(store.revocationReason).isEqualTo("logout");
        assertThat(store.events)
                .extracting(SecurityEvent::type)
                .containsExactly(SecurityEvent.Type.SESSION_REVOKED);
    }

    private static AccountRepository.CredentialAccount account(AccountStatus status) {
        return AccountRepository.CredentialAccount.restore(
                ACCOUNT_ID, AccountRole.CORREDOR, status, "stored-hash", 3L);
    }

    private static SessionRepository.StoredSession stored(
            OffsetDateTime lastUsedAt, OffsetDateTime absoluteExpiresAt, AccountStatus status) {
        return SessionRepository.StoredSession.restore(
                SESSION_ID,
                ACCOUNT_ID,
                lastUsedAt,
                absoluteExpiresAt,
                AccountRole.CORREDOR,
                status);
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

    private static final class TokensFake implements SessionTokenService {
        @Override
        public GeneratedSessionToken generate() {
            return new GeneratedSessionToken("raw-token", new byte[] {7, 8, 9});
        }

        @Override
        public byte[] verifier(String rawToken) {
            return new byte[] {4, 5, 6};
        }
    }

    private static final class PasswordsFake implements PasswordHasher {
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

    private static final class StoreFake
            implements AccountRepository,
                    SessionRepository,
                    LoginRateLimitRepository,
                    SecurityEventRepository {
        private Optional<CredentialAccount> account = Optional.empty();
        private Optional<StoredSession> storedSession = Optional.empty();
        private FailureCounts currentFailures = new FailureCounts(0, 0);
        private FailureCounts incrementedFailures = new FailureCounts(1, 1);
        private final List<SecurityEvent> events = new ArrayList<>();
        private String canonicalEmail;
        private String replacementHash;
        private byte[] createdVerifier;
        private OffsetDateTime absoluteExpiresAt;
        private OffsetDateTime touchedAt;
        private String revocationReason;
        private boolean passwordUpdated = true;
        private boolean sessionTouched = true;
        private boolean sessionRevoked = true;

        @Override
        public Optional<CredentialAccount> findCredentialAccount(String email) {
            canonicalEmail = email;
            return account;
        }

        @Override
        public boolean updatePasswordHash(
                CredentialAccount credentialAccount, String hash, OffsetDateTime changedAt) {
            replacementHash = hash;
            return passwordUpdated;
        }

        @Override
        public void create(
                SessionIdentity session,
                byte[] verifierHash,
                OffsetDateTime createdAt,
                OffsetDateTime expiresAt) {
            createdVerifier = verifierHash;
            absoluteExpiresAt = expiresAt;
        }

        @Override
        public Optional<StoredSession> findByVerifierForUpdate(byte[] verifierHash) {
            return storedSession;
        }

        @Override
        public boolean touch(UUID sessionId, OffsetDateTime lastUsedAt) {
            touchedAt = lastUsedAt;
            return sessionTouched;
        }

        @Override
        public boolean revoke(UUID sessionId, OffsetDateTime revokedAt, String reason) {
            revocationReason = reason;
            return sessionRevoked;
        }

        @Override
        public FailureCounts currentFailures(
                byte[] accountKey, byte[] ipKey, OffsetDateTime windowStart) {
            return currentFailures;
        }

        @Override
        public FailureCounts incrementFailures(
                byte[] accountKey,
                byte[] ipKey,
                OffsetDateTime windowStart,
                OffsetDateTime windowEnd,
                OffsetDateTime now) {
            return incrementedFailures;
        }

        @Override
        public void append(SecurityEvent event) {
            events.add(event);
        }
    }
}
