package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import java.beans.ConstructorProperties;
import java.util.Objects;
import java.util.UUID;

/** Persiste la cuenta pendiente y los hechos mínimos de una invitación inicial. */
public interface RunnerInvitationProvisioningRepository {
    ProvisionedRunnerAccount provision(PendingRunnerInvitation invitation);

    final class PendingRunnerInvitation {
        private final UUID accountId;
        private final UUID emailId;
        private final UUID invitationId;
        private final UUID declarationId;
        private final String presentationEmail;
        private final String canonicalEmail;
        private final UUID administratorAccountId;
        private final byte[] secretVerifier;
        private final UUID correlationId;

        @ConstructorProperties({
            "accountId",
            "emailId",
            "invitationId",
            "declarationId",
            "presentationEmail",
            "canonicalEmail",
            "administratorAccountId",
            "secretVerifier",
            "correlationId"
        })
        public PendingRunnerInvitation(
                UUID accountId,
                UUID emailId,
                UUID invitationId,
                UUID declarationId,
                String presentationEmail,
                String canonicalEmail,
                UUID administratorAccountId,
                byte[] secretVerifier,
                UUID correlationId) {
            this.accountId = Objects.requireNonNull(accountId);
            this.emailId = Objects.requireNonNull(emailId);
            this.invitationId = Objects.requireNonNull(invitationId);
            this.declarationId = Objects.requireNonNull(declarationId);
            this.presentationEmail = Objects.requireNonNull(presentationEmail);
            this.canonicalEmail = Objects.requireNonNull(canonicalEmail);
            this.administratorAccountId = Objects.requireNonNull(administratorAccountId);
            this.secretVerifier = Objects.requireNonNull(secretVerifier).clone();
            this.correlationId = Objects.requireNonNull(correlationId);
        }

        public UUID getAccountId() {
            return accountId;
        }

        public UUID getEmailId() {
            return emailId;
        }

        public UUID getInvitationId() {
            return invitationId;
        }

        public UUID getDeclarationId() {
            return declarationId;
        }

        public String getPresentationEmail() {
            return presentationEmail;
        }

        public String getCanonicalEmail() {
            return canonicalEmail;
        }

        public UUID getAdministratorAccountId() {
            return administratorAccountId;
        }

        public byte[] getSecretVerifier() {
            return secretVerifier.clone();
        }

        public UUID getCorrelationId() {
            return correlationId;
        }
    }
}
