package com.vgrunning.identityaccess.api.provisioning;

import java.util.UUID;

/** Hecho confirmado que permite activar de forma idempotente el perfil de corredor. */
public record RunnerInvitationActivated(UUID accountId, UUID correlationId) {}
