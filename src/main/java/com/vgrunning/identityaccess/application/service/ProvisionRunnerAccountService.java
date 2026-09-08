package com.vgrunning.identityaccess.application.service;

import com.vgrunning.identityaccess.api.provisioning.AccountProvisioningApi;
import com.vgrunning.identityaccess.api.provisioning.ProvisionRunnerAccount;
import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import com.vgrunning.identityaccess.application.exception.InvitationProvisioningForbiddenException;
import com.vgrunning.identityaccess.application.port.out.DigestPort;
import com.vgrunning.identityaccess.application.port.out.InvitationPayloadProtector;
import com.vgrunning.identityaccess.application.port.out.RunnerInvitationProvisioningRepository;
import com.vgrunning.identityaccess.application.port.out.SecretGenerator;
import com.vgrunning.identityaccess.domain.SealedPayload;
import com.vgrunning.identityaccess.domain.account.valueobject.EmailAddress;
import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;
import com.vgrunning.notificationdelivery.api.request.EncryptedValue;
import com.vgrunning.notificationdelivery.api.request.NotificationRequestApi;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Crea la identidad pendiente y deja una solicitud de correo en la misma transacción. */
@RequiredArgsConstructor
public class ProvisionRunnerAccountService implements AccountProvisioningApi {
    private final RunnerInvitationProvisioningRepository invitations;
    private final InvitationPayloadProtector protector;
    private final NotificationRequestApi notifications;
    private final DigestPort digest;
    private final SecretGenerator secrets;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ProvisionedRunnerAccount provision(ProvisionRunnerAccount command) {
        if (!command.actor().isAdministrator()) {
            throw new InvitationProvisioningForbiddenException();
        }
        EmailAddress email = EmailAddress.from(command.email());
        String presentationEmail = EmailAddress.presentationValue(command.email());
        UUID accountId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        String secret = secrets.generateUrlSafeSecret();
        ProvisionedRunnerAccount account =
                invitations.provision(
                        new RunnerInvitationProvisioningRepository.PendingRunnerInvitation(
                                accountId,
                                UUID.randomUUID(),
                                invitationId,
                                UUID.randomUUID(),
                                presentationEmail,
                                email.canonicalValue(),
                                command.actor().accountId(),
                                digest.sha256(secret),
                                command.correlationId()));
        notifications.create(
                new CreateNotificationRequest(
                        UUID.randomUUID(),
                        "invitation:" + invitationId,
                        invitationId,
                        toEncryptedValue(protector.protect(presentationEmail)),
                        toEncryptedValue(
                                protector.protect(
                                        String.format(
                                                Locale.ROOT,
                                                "/activar#i=%s&s=%s",
                                                invitationId,
                                                secret))),
                        command.correlationId()));
        return account;
    }

    private static EncryptedValue toEncryptedValue(SealedPayload payload) {
        return new EncryptedValue(payload.keyId(), payload.nonce(), payload.ciphertext());
    }
}
