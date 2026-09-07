package com.vgrunning.runnermanagement.application.port.in;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;

/** Aplica de forma idempotente la activación confirmada de un perfil pendiente. */
public interface ActivateRunnerUseCase {
    void activate(RunnerInvitationActivated event);
}
