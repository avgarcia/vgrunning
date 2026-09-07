package com.vgrunning.identityaccess.application.port.in;

import java.util.UUID;

/** Consume una invitación de activación sin crear una sesión HTTP. */
public interface AcceptInvitationUseCase {
    AcceptedInvitation accept(AcceptInvitation command);

    record AcceptInvitation(
            UUID invitationId, String secret, Boolean adultDeclarationConfirmed, String password) {}

    record AcceptedInvitation(UUID acceptanceId, String status) {}
}
