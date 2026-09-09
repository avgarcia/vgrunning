package com.vgrunning.identityaccess.domain;

import java.util.Objects;

/** Evidencia de la declaración de mayoría de edad que exige ADR-0010. */
public record AdultDeclaration(String actorKind, String origin, String textVersion) {
    public AdultDeclaration {
        Objects.requireNonNull(actorKind);
        Objects.requireNonNull(origin);
        Objects.requireNonNull(textVersion);
    }

    /** Declaración confirmada por la propia persona invitada durante su activación inicial. */
    public static AdultDeclaration initialActivation() {
        return new AdultDeclaration("invitee", "initial_activation", "ux-02-v0.1");
    }
}
