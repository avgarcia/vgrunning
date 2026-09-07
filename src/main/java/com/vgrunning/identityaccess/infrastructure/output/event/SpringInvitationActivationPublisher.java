package com.vgrunning.identityaccess.infrastructure.output.event;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;
import com.vgrunning.identityaccess.application.port.out.InvitationActivationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica el hecho dentro de la transacción para que Spring Modulith lo registre de forma durable.
 */
@RequiredArgsConstructor
public class SpringInvitationActivationPublisher implements InvitationActivationPublisher {
    private final ApplicationEventPublisher events;

    @Override
    public void publish(RunnerInvitationActivated event) {
        events.publishEvent(event);
    }
}
