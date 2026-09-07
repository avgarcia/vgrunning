package com.vgrunning.runnermanagement.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;
import com.vgrunning.runnermanagement.application.port.out.RunnerActivationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivateRunnerServiceTest {
    @Test
    void activatesTheRunnerUsingTheConfirmedIdentityEvent() {
        ActivationsFake activations = new ActivationsFake();
        RunnerInvitationActivated event =
                new RunnerInvitationActivated(UUID.randomUUID(), UUID.randomUUID());

        new ActivateRunnerService(activations).activate(event);

        assertThat(activations.accountId).isEqualTo(event.accountId());
        assertThat(activations.correlationId).isEqualTo(event.correlationId());
    }

    private static final class ActivationsFake implements RunnerActivationRepository {
        private UUID accountId;
        private UUID correlationId;

        @Override
        public void activate(UUID value, UUID correlation) {
            accountId = value;
            correlationId = correlation;
        }
    }
}
