package com.vgrunning.identityaccess.infrastructure.output.provider;

import com.vgrunning.identityaccess.application.port.out.ActivationLinkFactory;
import java.util.Locale;
import java.util.UUID;

/** Compone el fragmento de la ruta de activación del portal. */
public final class ActivationRouteLinkFactory implements ActivationLinkFactory {
    @Override
    public String activationFragment(UUID invitationId, String secret) {
        return String.format(Locale.ROOT, "/activar#i=%s&s=%s", invitationId, secret);
    }
}
