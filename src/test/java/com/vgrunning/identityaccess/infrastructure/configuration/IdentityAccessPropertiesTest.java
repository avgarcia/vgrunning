package com.vgrunning.identityaccess.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.infrastructure.configuration.synthetic.SyntheticAccountProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class IdentityAccessPropertiesTest {

    @Test
    void acceptsDeploymentSecretsAndAnExplicitRetention() {
        IdentityAccessProperties properties =
                new IdentityAccessProperties("synthetic-hmac-key", Duration.ofDays(90));

        assertThat(properties.rateLimitHmacKey()).isEqualTo("synthetic-hmac-key");
        assertThat(properties.securityEventRetention()).isEqualTo(Duration.ofDays(90));
    }

    @Test
    void rejectsMissingOrBlankHmacKeys() {
        assertThatThrownBy(() -> new IdentityAccessProperties(null, Duration.ofDays(90)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdentityAccessProperties("  ", Duration.ofDays(90)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingZeroOrNegativeRetention() {
        assertThatThrownBy(() -> new IdentityAccessProperties("key", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdentityAccessProperties("key", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdentityAccessProperties("key", Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesBothSyntheticPasswordsIndependently() {
        SyntheticAccountProperties properties =
                new SyntheticAccountProperties("administrator-password", "runner-password");
        assertThat(properties.administratorPassword()).isEqualTo("administrator-password");
        assertThat(properties.runnerPassword()).isEqualTo("runner-password");

        assertThatThrownBy(() -> new SyntheticAccountProperties(null, "runner"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SyntheticAccountProperties(" ", "runner"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SyntheticAccountProperties("administrator", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SyntheticAccountProperties("administrator", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
