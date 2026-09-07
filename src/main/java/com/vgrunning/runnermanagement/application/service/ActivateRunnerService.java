package com.vgrunning.runnermanagement.application.service;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;
import com.vgrunning.runnermanagement.application.port.in.ActivateRunnerUseCase;
import com.vgrunning.runnermanagement.application.port.out.RunnerActivationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Coordina la transición segura de pendiente a activo tras confirmar identidad. */
@RequiredArgsConstructor
public class ActivateRunnerService implements ActivateRunnerUseCase {
    private final RunnerActivationRepository runners;

    @Override
    @Transactional
    public void activate(RunnerInvitationActivated event) {
        runners.activate(event.accountId(), event.correlationId());
    }
}
