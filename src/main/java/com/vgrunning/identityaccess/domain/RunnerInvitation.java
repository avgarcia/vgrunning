package com.vgrunning.identityaccess.domain;

import com.vgrunning.identityaccess.domain.account.valueobject.EmailAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/** Identidad y credenciales de una invitación de alta que se está creando. */
// El verificador se clona al construir y al leer, y equals/hashCode comparan su contenido: el
// footgun que evita esta regla ya está cubierto explícitamente más abajo.
@SuppressWarnings("ArrayRecordComponent")
public record RunnerInvitation(
        UUID accountId,
        UUID emailId,
        UUID invitationId,
        UUID declarationId,
        String presentationEmail,
        String canonicalEmail,
        UUID administratorAccountId,
        byte[] secretVerifier,
        UUID correlationId) {

    public RunnerInvitation {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(emailId);
        Objects.requireNonNull(invitationId);
        Objects.requireNonNull(declarationId);
        Objects.requireNonNull(presentationEmail);
        Objects.requireNonNull(canonicalEmail);
        Objects.requireNonNull(administratorAccountId);
        secretVerifier = Objects.requireNonNull(secretVerifier).clone();
        Objects.requireNonNull(correlationId);
    }

    /** Genera los identificadores de una invitación nueva a partir de datos ya validados. */
    public static RunnerInvitation create(
            EmailAddress email,
            String presentationEmail,
            UUID administratorAccountId,
            byte[] secretVerifier,
            UUID correlationId) {
        return new RunnerInvitation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                presentationEmail,
                email.canonicalValue(),
                administratorAccountId,
                secretVerifier,
                correlationId);
    }

    @Override
    public byte[] secretVerifier() {
        return secretVerifier.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RunnerInvitation that
                && accountId.equals(that.accountId)
                && emailId.equals(that.emailId)
                && invitationId.equals(that.invitationId)
                && declarationId.equals(that.declarationId)
                && presentationEmail.equals(that.presentationEmail)
                && canonicalEmail.equals(that.canonicalEmail)
                && administratorAccountId.equals(that.administratorAccountId)
                && Arrays.equals(secretVerifier, that.secretVerifier)
                && correlationId.equals(that.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                accountId,
                emailId,
                invitationId,
                declarationId,
                presentationEmail,
                canonicalEmail,
                administratorAccountId,
                Arrays.hashCode(secretVerifier),
                correlationId);
    }
}
