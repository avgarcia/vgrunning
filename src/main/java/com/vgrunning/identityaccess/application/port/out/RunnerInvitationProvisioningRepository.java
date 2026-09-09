package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import com.vgrunning.identityaccess.domain.RunnerInvitation;

/** Persiste la cuenta pendiente y los hechos mínimos de una invitación inicial. */
public interface RunnerInvitationProvisioningRepository {
    ProvisionedRunnerAccount provision(RunnerInvitation invitation);
}
