package com.vgrunning.identityaccess.api.provisioning;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import java.util.UUID;

/** Datos mínimos que corredores entrega a identidad para una invitación inicial. */
public record ProvisionRunnerAccount(String email, ActorContext actor, UUID correlationId) {}
