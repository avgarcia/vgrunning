package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;

/** Publica el hecho confirmado de activación sin acoplar la aplicación a Spring Modulith. */
public interface InvitationActivationPublisher {
    void publish(RunnerInvitationActivated event);
}
