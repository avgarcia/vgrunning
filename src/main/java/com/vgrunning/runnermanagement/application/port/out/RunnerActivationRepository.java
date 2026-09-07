package com.vgrunning.runnermanagement.application.port.out;

import java.util.UUID;

/** Actualiza el perfil pendiente al recibir una activación confirmada de identidad. */
public interface RunnerActivationRepository {
    void activate(UUID accountId, UUID correlationId);
}
