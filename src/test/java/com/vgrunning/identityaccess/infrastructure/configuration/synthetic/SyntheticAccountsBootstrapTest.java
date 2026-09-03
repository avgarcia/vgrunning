package com.vgrunning.identityaccess.infrastructure.configuration.synthetic;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.infrastructure.output.persistence.jooq.JooqSyntheticAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SyntheticAccountsBootstrapTest {
    @Test
    void provisionsBothConfiguredAccountsThroughTheJooqComponent() {
        AccountsFake accounts = new AccountsFake();
        SyntheticAccountsBootstrap bootstrap =
                new SyntheticAccountsBootstrap(
                        accounts,
                        new PasswordsFake(),
                        new SyntheticAccountProperties("admin-pa\u0301ss", "runner-pa\u0301ss"));

        bootstrap.run(new org.springframework.boot.DefaultApplicationArguments());

        assertThat(accounts.provisions).hasSize(2);
        assertThat(accounts.provisions)
                .extracting(JooqSyntheticAccountRepository.SyntheticAccountProvision::role)
                .containsExactly(AccountRole.ADMINISTRADOR, AccountRole.CORREDOR);
        assertThat(accounts.provisions)
                .extracting(
                        JooqSyntheticAccountRepository.SyntheticAccountProvision::canonicalEmail)
                .containsExactly(
                        "administrator@running-coach.invalid", "runner@running-coach.invalid");
        assertThat(accounts.provisions)
                .extracting(JooqSyntheticAccountRepository.SyntheticAccountProvision::passwordHash)
                .containsExactly("hash:admin-páss", "hash:runner-páss");
    }

    private static final class AccountsFake extends JooqSyntheticAccountRepository {
        private List<SyntheticAccountProvision> provisions;

        AccountsFake() {
            super(null, null);
        }

        @Override
        public void provisionAll(List<SyntheticAccountProvision> values) {
            provisions = values;
        }
    }

    private static final class PasswordsFake implements PasswordHasher {
        @Override
        public boolean matchesForAuthentication(String password, Optional<String> encodedPassword) {
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
    }
}
