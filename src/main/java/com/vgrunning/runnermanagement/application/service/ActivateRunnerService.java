package com.vgrunning.runnermanagement.application.service;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;
import com.vgrunning.runnermanagement.application.port.in.ActivateRunnerUseCase;
import com.vgrunning.runnermanagement.application.port.out.RunnerActivationRepository;
import org.springframework.transaction.annotation.Transactional;

/** Coordina la transición segura de pendiente a activo tras confirmar identidad. */
public class ActivateRunnerService implements ActivateRunnerUseCase {
    private final RunnerActivationRepository runners;

    public ActivateRunnerService(RunnerActivationRepository runners) {
        this.runners = runners;
    }

    @Override
    @Transactional
    public void activate(RunnerInvitationActivated event) {
        runners.activate(event.accountId(), event.correlationId());
    }
}
