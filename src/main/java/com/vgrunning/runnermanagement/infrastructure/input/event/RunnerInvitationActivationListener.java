package com.vgrunning.runnermanagement.infrastructure.input.event;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;
import com.vgrunning.runnermanagement.application.port.in.ActivateRunnerUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Consume el hecho durable de identidad sin conceder acceso operativo antes del commit. */
@RequiredArgsConstructor
@Component
public class RunnerInvitationActivationListener {
    private final ActivateRunnerUseCase activation;

    @ApplicationModuleListener
    void on(RunnerInvitationActivated event) {
        activation.activate(event);
    }
}
