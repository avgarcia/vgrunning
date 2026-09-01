package com.vgrunning.identityaccess.domain.securityevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class SecurityEventRetentionTest {

    @Test
    void calculatesRetentionFromTheEventInstant() {
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-31T08:00:00Z");
        SecurityEventRetention retention = new SecurityEventRetention(Duration.ofDays(90));

        assertThat(retention.retentionUntil(occurredAt)).isEqualTo(occurredAt.plusDays(90));
    }

    @Test
    void rejectsMissingZeroAndNegativeRetention() {
        assertThatThrownBy(() -> new SecurityEventRetention(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecurityEventRetention(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecurityEventRetention(Duration.ofDays(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
