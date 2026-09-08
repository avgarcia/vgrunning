package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.domain.SealedPayload;

/** Cifra el destino y enlace de una invitación antes de persistir la solicitud de correo. */
public interface InvitationPayloadProtector {
    SealedPayload protect(String plaintext);
}
