package com.vgrunning.identityaccess.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.port.in.CreateSessionUseCase.AuthenticatedAccount;
import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prueba unitaria de credenciales: Spring Session gestiona la sesión HTTP. */
class CreateSessionServiceTest {
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private AccountsFake accounts;
    private PasswordsFake passwords;
    private CreateSessionService useCase;

    @BeforeEach
    void setUp() {
        accounts = new AccountsFake();
        passwords = new PasswordsFake();
        useCase = new CreateSessionService(accounts, passwords);
    }

    @Test
    void authenticatesNormalizedCredentialsAndRehashesWhenNeeded() {
        accounts.account = Optional.of(account(AccountStatus.ACTIVE));
        passwords.matches = true;
        passwords.needsRehash = true;

        AuthenticatedAccount login = useCase.create(" RUNNER@example.invalid ", "pa\u0301ss");

        assertThat(login.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(accounts.canonicalEmail).isEqualTo("runner@example.invalid");
        assertThat(passwords.verifiedPassword).isEqualTo("páss");
        assertThat(accounts.replacementHash).isEqualTo("new-hash:páss");
    }

    @Test
    void returnsTheSameFailureForUnknownAndInactiveAccounts() {
        passwords.matches = false;
        assertThatThrownBy(() -> useCase.create("runner@example.invalid", "password"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(passwords.verifiedHash).isEqualTo("dummy-hash");

        accounts.account = Optional.of(account(AccountStatus.DISABLED));
        passwords.matches = true;
        assertThatThrownBy(() -> useCase.create("runner@example.invalid", "password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void failsWhenTheOptimisticPasswordUpdateLosesTheRace() {
        accounts.account = Optional.of(account(AccountStatus.ACTIVE));
        accounts.passwordUpdated = false;
        passwords.matches = true;
        passwords.needsRehash = true;

        assertThatThrownBy(() -> useCase.create("runner@example.invalid", "password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cuenta cambió");
    }

    private static AccountRepository.CredentialAccount account(AccountStatus status) {
        return AccountRepository.CredentialAccount.restore(
                ACCOUNT_ID, AccountRole.CORREDOR, status, "stored-hash", 3L);
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

    private static final class AccountsFake implements AccountRepository {
        private Optional<CredentialAccount> account = Optional.empty();
        private String canonicalEmail;
        private String replacementHash;
        private boolean passwordUpdated = true;

        @Override
        public Optional<CredentialAccount> findCredentialAccount(String email) {
            canonicalEmail = email;
            return account;
        }

        @Override
        public boolean updatePasswordHash(CredentialAccount account, String hash) {
            replacementHash = hash;
            return passwordUpdated;
        }
    }
}
