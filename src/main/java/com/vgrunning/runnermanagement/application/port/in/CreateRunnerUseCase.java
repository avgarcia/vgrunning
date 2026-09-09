package com.vgrunning.runnermanagement.application.port.in;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import com.vgrunning.runnermanagement.domain.Runner;
import java.util.UUID;

/** Caso de uso protegido que crea un corredor pendiente de activación. */
public interface CreateRunnerUseCase {
    Runner create(ActorContext actor, UUID idempotencyKey, CreateRunner command);

    record CreateRunner(
            String givenName, String familyName, String email, Boolean adultDeclarationConfirmed) {}
}
