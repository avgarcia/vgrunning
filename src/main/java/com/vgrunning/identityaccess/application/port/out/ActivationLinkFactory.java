package com.vgrunning.identityaccess.application.port.out;

import java.util.UUID;

/** Construye el fragmento de enlace de activación que recibe la persona invitada. */
public interface ActivationLinkFactory {
    String activationFragment(UUID invitationId, String secret);
}
