package com.vgrunning.identityaccess.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.infrastructure.configuration.synthetic.SyntheticAccountProperties;
import org.junit.jupiter.api.Test;

class IdentityAccessPropertiesTest {

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
