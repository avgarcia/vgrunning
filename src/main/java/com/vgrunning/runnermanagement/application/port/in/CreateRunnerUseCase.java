package com.vgrunning.runnermanagement.application.port.in;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import java.util.UUID;

/** Caso de uso protegido que crea un corredor pendiente de activación. */
public interface CreateRunnerUseCase {
    CreatedRunner create(ActorContext actor, UUID idempotencyKey, CreateRunner command);

    record CreateRunner(
            String givenName, String familyName, String email, Boolean adultDeclarationConfirmed) {}

    record CreatedRunner(UUID id, String givenName, String familyName, String status) {}
}
