package com.vgrunning.identityaccess.application.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvitationExceptionsTest {
    @Test
    void exposesOnlyTheStablePublicInvitationFailures() {
        assertThat(new EmailAlreadyReservedException())
                .extracting(EmailAlreadyReservedException::code, Throwable::getMessage)
                .containsExactly("email_already_reserved", "El correo ya está reservado.");
        assertThat(new InvalidInvitationAcceptanceException())
                .extracting(InvalidInvitationAcceptanceException::code, Throwable::getMessage)
                .containsExactly("invalid_request", "La solicitud no es válida.");
        assertThat(new InvitationNotAvailableException())
                .extracting(InvitationNotAvailableException::code, Throwable::getMessage)
                .containsExactly("invitation_not_available", "La invitación no está disponible.");
    }
}
