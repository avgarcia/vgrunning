package com.vgrunning.identityaccess.api.provisioning;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Identificadores opacos creados por identidad; no contiene correo ni secreto. */
public record ProvisionedRunnerAccount(
        UUID accountId, UUID invitationId, OffsetDateTime activationExpiresAt) {}
