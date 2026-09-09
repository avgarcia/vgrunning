package com.vgrunning.identityaccess.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.api.actor.ActorContext;
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
import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;
import com.vgrunning.notificationdelivery.api.request.NotificationRequestApi;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProvisionRunnerAccountServiceTest {
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void provisionsAPendingAccountAndAnEncryptedInvitationRequest() {
        InvitationsFake invitations = new InvitationsFake();
        ProtectorFake protector = new ProtectorFake();
        NotificationsFake notifications = new NotificationsFake();
        ProvisionRunnerAccountService service =
                new ProvisionRunnerAccountService(
                        invitations,
                        protector,
                        notifications,
                        digest(),
                        secrets(),
                        activationLinks());

        ProvisionedRunnerAccount account =
                service.provision(
                        new ProvisionRunnerAccount(
                                " Lucía@example.invalid ",
                                new ActorContext(ADMIN_ID, "administrador"),
                                UUID.fromString("10000000-0000-0000-0000-000000000002")));

        assertThat(invitations.invitation.presentationEmail()).isEqualTo("Lucía@example.invalid");
        assertThat(invitations.invitation.canonicalEmail()).isEqualTo("lucía@example.invalid");
        assertThat(invitations.invitation.secretVerifier()).hasSize(32);
        assertThat(account.invitationId()).isEqualTo(invitations.invitation.invitationId());
        assertThat(notifications.request.logicalKey())
                .isEqualTo("invitation:" + account.invitationId());
        assertThat(protector.values)
                .anySatisfy(
                        value ->
                                assertThat(value)
                                        .startsWith(
                                                "/activar#i=" + account.invitationId() + "&s="));
    }

    @Test
    void rejectsNonAdministratorsWithoutPersistingOrSendingAnything() {
        InvitationsFake invitations = new InvitationsFake();
        NotificationsFake notifications = new NotificationsFake();
        ProvisionRunnerAccountService service =
                new ProvisionRunnerAccountService(
                        invitations,
                        new ProtectorFake(),
                        notifications,
                        digest(),
                        secrets(),
                        activationLinks());

        assertThatThrownBy(
                        () ->
                                service.provision(
                                        new ProvisionRunnerAccount(
                                                "runner@example.invalid",
                                                new ActorContext(ADMIN_ID, "corredor"),
                                                UUID.randomUUID())))
                .isInstanceOfSatisfying(
                        InvitationProvisioningForbiddenException.class,
                        exception -> {
                            assertThat(exception.code())
                                    .isEqualTo("invitation_provisioning_forbidden");
                            assertThat(exception.getMessage()).contains("permiso");
                        });
        assertThat(invitations.invitation).isNull();
        assertThat(notifications.request).isNull();
    }

    private static DigestPort digest() {
        return value -> {
            try {
                return MessageDigest.getInstance("SHA-256")
                        .digest(value.getBytes(StandardCharsets.UTF_8));
            } catch (java.security.NoSuchAlgorithmException exception) {
                throw new IllegalStateException(exception);
            }
        };
    }

    private static SecretGenerator secrets() {
        return () -> "test-secret";
    }

    private static ActivationLinkFactory activationLinks() {
        return (invitationId, secret) -> "/activar#i=" + invitationId + "&s=" + secret;
    }

    private static final class InvitationsFake implements RunnerInvitationProvisioningRepository {
        private RunnerInvitation invitation;

        @Override
        public ProvisionedRunnerAccount provision(RunnerInvitation value) {
            invitation = value;
            return new ProvisionedRunnerAccount(
                    value.accountId(),
                    value.invitationId(),
                    OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
        }
    }

    private static final class ProtectorFake implements InvitationPayloadProtector {
        private final List<String> values = new ArrayList<>();

        @Override
        public SealedPayload protect(String value) {
            values.add(value);
            return new SealedPayload("test", new byte[] {1}, new byte[] {2});
        }
    }

    private static final class NotificationsFake implements NotificationRequestApi {
        private CreateNotificationRequest request;

        @Override
        public void create(CreateNotificationRequest value) {
            request = value;
        }
    }
}
