package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.notificationdelivery.api.request.EncryptedValue;

/** Cifra el destino y enlace de una invitación antes de persistir la solicitud de correo. */
public interface InvitationPayloadProtector {
    EncryptedValue protect(String plaintext);
}
