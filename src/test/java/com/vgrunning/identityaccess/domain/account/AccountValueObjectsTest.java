package com.vgrunning.identityaccess.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AccountValueObjectsTest {

    @Test
    void resolvesEveryPersistedRole() {
        for (AccountRole role : AccountRole.values()) {
            assertThat(AccountRole.fromValue(role.value())).isSameAs(role);
        }
    }

    @Test
    void rejectsAnUnknownRole() {
        assertThatThrownBy(() -> AccountRole.fromValue("visitante"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("visitante");
    }

    @Test
    void resolvesEveryPersistedStatus() {
        for (AccountStatus status : AccountStatus.values()) {
            assertThat(AccountStatus.fromValue(status.value())).isSameAs(status);
        }
    }

    @Test
    void rejectsAnUnknownStatus() {
        assertThatThrownBy(() -> AccountStatus.fromValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void canonicalizesEmailWithoutProviderSpecificRules() {
        EmailAddress email = EmailAddress.from("  RU\u0301NNER+Club@Example.Invalid  ");

        assertThat(email.canonicalValue()).isEqualTo("r\u00FAnner+club@example.invalid");
    }

    @Test
    void rejectsABlankCanonicalEmail() {
        assertThatThrownBy(() -> EmailAddress.from("  \t  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }
}
