package com.vgrunning.identityaccess.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.port.out.SyntheticAccountRepository;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SyntheticAccountProvisioningServiceTest {
    @Test
    void provisionsBothConfiguredAccountsThroughTheOutputPort() {
        AccountsFake accounts = new AccountsFake();
        SyntheticAccountProvisioningService service =
                new SyntheticAccountProvisioningService(accounts, new PasswordsFake());

        service.provision(
                new com.vgrunning.identityaccess.application.port.in.ProvisionSyntheticAccountsUseCase.Command(
                        "admin-pa\u0301ss", "runner-pa\u0301ss"));

        assertThat(accounts.provisions).hasSize(2);
        assertThat(accounts.provisions)
                .extracting(Provision::role)
                .containsExactly(AccountRole.ADMINISTRADOR, AccountRole.CORREDOR);
        assertThat(accounts.provisions)
                .extracting(Provision::canonicalEmail)
                .containsExactly("administrator@running-coach.invalid", "runner@running-coach.invalid");
        assertThat(accounts.provisions)
                .extracting(Provision::passwordHash)
                .containsExactly("hash:admin-páss", "hash:runner-páss");
    }

    private record Provision(
            UUID accountId,
            AccountRole role,
            String presentationEmail,
            String canonicalEmail,
            String passwordHash,
            OffsetDateTime now) {}

    private static final class AccountsFake implements SyntheticAccountRepository {
        private final List<Provision> provisions = new ArrayList<>();

        @Override
        public boolean provision(
                UUID accountId,
                AccountRole role,
                String presentationEmail,
                String canonicalEmail,
                String passwordHash,
                OffsetDateTime now) {
            provisions.add(
                    new Provision(
                            accountId,
                            role,
                            presentationEmail,
                            canonicalEmail,
                            passwordHash,
                            now));
            return true;
        }
    }

    private static final class PasswordsFake implements PasswordHasher {
        @Override
        public boolean matches(String password, String encodedPassword) {
            return false;
        }

        @Override
        public String hash(String password) {
            return "hash:" + password;
        }

        @Override
        public boolean needsRehash(String encodedPassword) {
            return false;
        }

        @Override
        public String dummyHash() {
            return "dummy";
        }
    }
}
