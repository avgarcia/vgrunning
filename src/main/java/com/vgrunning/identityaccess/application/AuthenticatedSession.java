package com.vgrunning.identityaccess.application;

import java.util.UUID;

/** Identidad autenticada que solo vive durante el procesamiento de una petición HTTP. */
public record AuthenticatedSession(UUID sessionId, UUID accountId, String role, String status) {}
