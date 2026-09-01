package com.vgrunning.identityaccess.domain.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SessionSecurityPolicyTest {

    @Test
    void acceptsPositiveDurationsAndLimits() {
        SessionSecurityPolicy policy =
                new SessionSecurityPolicy(
                        Duration.ofHours(12), Duration.ofDays(7), Duration.ofMinutes(15), 5, 20);

        assertThat(policy.idleTimeout()).isEqualTo(Duration.ofHours(12));
        assertThat(policy.absoluteTimeout()).isEqualTo(Duration.ofDays(7));
        assertThat(policy.rateWindow()).isEqualTo(Duration.ofMinutes(15));
        assertThat(policy.accountFailureLimit()).isEqualTo(5);
        assertThat(policy.ipFailureLimit()).isEqualTo(20);
    }

    @Test
    void rejectsMissingZeroAndNegativeDurations() {
        assertThatThrownBy(
                        () ->
                                new SessionSecurityPolicy(
                                        null, Duration.ofDays(7), Duration.ofMinutes(15), 5, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idleTimeout");
        assertThatThrownBy(
                        () ->
                                new SessionSecurityPolicy(
                                        Duration.ofHours(12),
                                        Duration.ZERO,
                                        Duration.ofMinutes(15),
                                        5,
                                        20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absoluteTimeout");
        assertThatThrownBy(
                        () ->
                                new SessionSecurityPolicy(
                                        Duration.ofHours(12),
                                        Duration.ofDays(7),
                                        Duration.ofMinutes(-1),
                                        5,
                                        20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rateWindow");
    }

    @Test
    void rejectsNonPositiveFailureLimitsIndependently() {
        assertThatThrownBy(
                        () ->
                                new SessionSecurityPolicy(
                                        Duration.ofHours(12),
                                        Duration.ofDays(7),
                                        Duration.ofMinutes(15),
                                        0,
                                        20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límites");
        assertThatThrownBy(
                        () ->
                                new SessionSecurityPolicy(
                                        Duration.ofHours(12),
                                        Duration.ofDays(7),
                                        Duration.ofMinutes(15),
                                        5,
                                        -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límites");
    }
}
