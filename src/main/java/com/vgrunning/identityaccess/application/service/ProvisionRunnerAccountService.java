package com.vgrunning.identityaccess.application.service;

import com.vgrunning.identityaccess.api.provisioning.AccountProvisioningApi;
import com.vgrunning.identityaccess.api.provisioning.ProvisionRunnerAccount;
import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import com.vgrunning.identityaccess.application.exception.InvitationProvisioningForbiddenException;
import com.vgrunning.identityaccess.application.port.out.InvitationPayloadProtector;
import com.vgrunning.identityaccess.application.port.out.RunnerInvitationProvisioningRepository;
import com.vgrunning.identityaccess.domain.account.valueobject.EmailAddress;
import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;
import com.vgrunning.notificationdelivery.api.request.NotificationRequestApi;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Crea la identidad pendiente y deja una solicitud de correo en la misma transacción. */
@RequiredArgsConstructor
public class ProvisionRunnerAccountService implements AccountProvisioningApi {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RunnerInvitationProvisioningRepository invitations;
    private final InvitationPayloadProtector protector;
    private final NotificationRequestApi notifications;

    @Override
    public ProvisionedRunnerAccount provision(ProvisionRunnerAccount command) {
        if (!command.actor().isAdministrator()) {
            throw new InvitationProvisioningForbiddenException();
        }
        EmailAddress email = EmailAddress.from(command.email());
        String presentationEmail =
                Normalizer.normalize(command.email().strip(), Normalizer.Form.NFC);
        UUID accountId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        String secret = secret();
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
                                sha256(secret),
                                command.correlationId()));
        notifications.create(
                new CreateNotificationRequest(
                        UUID.randomUUID(),
                        "invitation:" + invitationId,
                        invitationId,
                        protector.protect(presentationEmail),
                        protector.protect(
                                String.format(
                                        Locale.ROOT, "/activar#i=%s&s=%s", invitationId, secret)),
                        command.correlationId()));
        return account;
    }

    private static String secret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
