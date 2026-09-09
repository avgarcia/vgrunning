package com.vgrunning.identityaccess.application.service;

import com.vgrunning.identityaccess.api.provisioning.AccountProvisioningApi;
import com.vgrunning.identityaccess.api.provisioning.ProvisionRunnerAccount;
import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import com.vgrunning.identityaccess.application.exception.InvitationProvisioningForbiddenException;
import com.vgrunning.identityaccess.application.port.out.ActivationLinkFactory;
import com.vgrunning.identityaccess.application.port.out.DigestPort;
import com.vgrunning.identityaccess.application.port.out.InvitationPayloadProtector;
import com.vgrunning.identityaccess.application.port.out.RunnerInvitationProvisioningRepository;
import com.vgrunning.identityaccess.application.port.out.SecretGenerator;
import com.vgrunning.identityaccess.domain.RunnerInvitation;
import com.vgrunning.identityaccess.domain.SealedPayload;
import com.vgrunning.identityaccess.domain.account.valueobject.EmailAddress;
import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;
import com.vgrunning.notificationdelivery.api.request.EncryptedValue;
import com.vgrunning.notificationdelivery.api.request.NotificationRequestApi;
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
    private final ActivationLinkFactory activationLinks;

    /**
     * Crea la cuenta pendiente de un corredor y deja lista, en la misma transacción, la solicitud
     * de correo con su enlace de activación.
     *
     * @param command correo del corredor y actor que solicita el alta, ya autenticado
     * @return identidad de la cuenta y de la invitación creadas
     * @throws InvitationProvisioningForbiddenException si el actor no es administrador
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ProvisionedRunnerAccount provision(ProvisionRunnerAccount command) {
        command.actor().requireAdministrator(InvitationProvisioningForbiddenException::new);
        EmailAddress email = EmailAddress.from(command.email());
        String presentationEmail = EmailAddress.presentationValue(command.email());
        String secret = secrets.generateUrlSafeSecret();
        RunnerInvitation invitation =
                RunnerInvitation.create(
                        email,
                        presentationEmail,
                        command.actor().accountId(),
                        digest.sha256(secret),
                        command.correlationId());
        ProvisionedRunnerAccount account = invitations.provision(invitation);
        notifications.create(buildActivationNotificationRequest(invitation, secret));
        return account;
    }

    /** Cifra el correo y el enlace de activación de la invitación en una solicitud de correo. */
    private CreateNotificationRequest buildActivationNotificationRequest(
            RunnerInvitation invitation, String secret) {
        return new CreateNotificationRequest(
                UUID.randomUUID(),
                "invitation:" + invitation.invitationId(),
                invitation.invitationId(),
                toEncryptedValue(protector.protect(invitation.presentationEmail())),
                toEncryptedValue(
                        protector.protect(
                                activationLinks.activationFragment(
                                        invitation.invitationId(), secret))),
                invitation.correlationId());
    }

    /** Convierte el sobre cifrado propio del módulo al tipo publicado por notification-delivery. */
    private static EncryptedValue toEncryptedValue(SealedPayload payload) {
        return new EncryptedValue(payload.keyId(), payload.nonce(), payload.ciphertext());
    }
}
