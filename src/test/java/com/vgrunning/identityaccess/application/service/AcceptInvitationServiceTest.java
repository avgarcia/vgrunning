package com.vgrunning.identityaccess.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;
import com.vgrunning.identityaccess.application.exception.InvalidInvitationAcceptanceException;
import com.vgrunning.identityaccess.application.exception.InvitationNotAvailableException;
import com.vgrunning.identityaccess.application.mapper.InvitationActivationMapper;
import com.vgrunning.identityaccess.application.port.in.AcceptInvitationUseCase.AcceptInvitation;
import com.vgrunning.identityaccess.application.port.out.DigestPort;
import com.vgrunning.identityaccess.application.port.out.InvitationActivationPublisher;
import com.vgrunning.identityaccess.application.port.out.InvitationRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.domain.AdultDeclaration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Prueba el consumo de una invitación sin crear sesión ni revelar el secreto. */
class AcceptInvitationServiceTest {
    private static final UUID INVITATION_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final InvitationActivationMapper ACTIVATION_MAPPER =
            Mappers.getMapper(InvitationActivationMapper.class);

    @Test
    void acceptsAValidInvitationAndPublishesActivationAfterPersistence() {
        InvitationsFake invitations = new InvitationsFake(sha256("secret"));
        EventsFake events = new EventsFake();
        AcceptInvitationService service =
                new AcceptInvitationService(
                        invitations, events, passwords(), ACTIVATION_MAPPER, digest(), clock());

        var accepted =
                service.accept(
                        new AcceptInvitation(INVITATION_ID, "secret", true, "long-password"));

        assertThat(accepted.status()).isEqualTo("accepted");
        assertThat(invitations.passwordHash).isEqualTo("hash:long-password");
        assertThat(events.event.accountId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void rejectsMissingSecretsBeforeQueryingPersistence() {
        InvitationsFake invitations = new InvitationsFake(sha256("secret"));
        AcceptInvitationService service =
                new AcceptInvitationService(
                        invitations,
                        event -> {},
                        passwords(),
                        ACTIVATION_MAPPER,
                        digest(),
                        clock());

        assertThatThrownBy(
                        () ->
                                service.accept(
                                        new AcceptInvitation(
                                                INVITATION_ID, null, true, "long-password")))
                .isInstanceOf(InvalidInvitationAcceptanceException.class);
        assertThat(invitations.lookups).isZero();
    }

    @Test
    void rejectsUnavailableAndMismatchedInvitationsWithoutPublishingAnEvent() {
        InvitationsFake unavailable = new InvitationsFake(sha256("secret"));
        unavailable.available = false;
        EventsFake events = new EventsFake();
        AcceptInvitationService unavailableService =
                new AcceptInvitationService(
                        unavailable, events, passwords(), ACTIVATION_MAPPER, digest(), clock());

        assertThatThrownBy(
                        () ->
                                unavailableService.accept(
                                        new AcceptInvitation(
                                                INVITATION_ID, "secret", true, "long-password")))
                .isInstanceOf(InvitationNotAvailableException.class);
        assertThat(events.event).isNull();

        InvitationsFake mismatched = new InvitationsFake(sha256("secret"));
        AcceptInvitationService mismatchedService =
                new AcceptInvitationService(
                        mismatched, events, passwords(), ACTIVATION_MAPPER, digest(), clock());

        assertThatThrownBy(
                        () ->
                                mismatchedService.accept(
                                        new AcceptInvitation(
                                                INVITATION_ID, "other", true, "long-password")))
                .isInstanceOf(InvitationNotAvailableException.class);
        assertThat(mismatched.passwordHash).isNull();
    }

    @Test
    void rejectsAnExpiredInvitationBeforeCheckingTheSecret() {
        InvitationsFake expired =
                new InvitationsFake(sha256("secret"), OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        AcceptInvitationService service =
                new AcceptInvitationService(
                        expired, event -> {}, passwords(), ACTIVATION_MAPPER, digest(), clock());

        assertThatThrownBy(
                        () ->
                                service.accept(
                                        new AcceptInvitation(
                                                INVITATION_ID, "secret", true, "long-password")))
                .isInstanceOf(InvitationNotAvailableException.class);
        assertThat(expired.passwordHash).isNull();
    }

    @Test
    void rejectsInvalidDeclarationsAndPasswordLengthsBeforeAccepting() {
        InvitationsFake invitations = new InvitationsFake(sha256("secret"));
        AcceptInvitationService service =
                new AcceptInvitationService(
                        invitations,
                        event -> {},
                        passwords(),
                        ACTIVATION_MAPPER,
                        digest(),
                        clock());

        assertThatThrownBy(
                        () ->
                                service.accept(
                                        new AcceptInvitation(
                                                INVITATION_ID, "secret", false, "long-password")))
                .isInstanceOf(InvalidInvitationAcceptanceException.class);
        assertThatThrownBy(
                        () ->
                                service.accept(
                                        new AcceptInvitation(
                                                INVITATION_ID, "secret", true, "short")))
                .isInstanceOf(InvalidInvitationAcceptanceException.class);
        assertThatThrownBy(
                        () ->
                                service.accept(
                                        new AcceptInvitation(
                                                INVITATION_ID, "secret", true, "x".repeat(129))))
                .isInstanceOf(InvalidInvitationAcceptanceException.class);
        assertThat(invitations.passwordHash).isNull();
    }

    @Test
    void rejectsWhenTheRepositoryCannotApplyTheTransition() {
        InvitationsFake invitations = new InvitationsFake(sha256("secret"));
        invitations.acceptSucceeds = false;
        AcceptInvitationService service =
                new AcceptInvitationService(
                        invitations,
                        event -> {},
                        passwords(),
                        ACTIVATION_MAPPER,
                        digest(),
                        clock());

        assertThatThrownBy(
                        () ->
                                service.accept(
                                        new AcceptInvitation(
                                                INVITATION_ID, "secret", true, "long-password")))
                .isInstanceOf(InvitationNotAvailableException.class);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static DigestPort digest() {
        return AcceptInvitationServiceTest::sha256;
    }

    private static Clock clock() {
        return Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"), ZoneOffset.UTC);
    }

    private static PasswordHasher passwords() {
        return new PasswordHasher() {
            @Override
            public boolean matchesForAuthentication(
                    String password, Optional<String> encodedPassword) {
                return false;
            }

            @Override
            public String hash(String password) {
                return "hash:" + password;
            }

            @Override
            public boolean needsRehash(String encodedPassword) {
                return false;
            }
        };
    }

    private static final class InvitationsFake implements InvitationRepository {
        private final ActivationInvitation invitation;
        private int lookups;
        private String passwordHash;
        private boolean available = true;
        private boolean acceptSucceeds = true;

        private InvitationsFake(byte[] verifier) {
            this(verifier, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        }

        private InvitationsFake(byte[] verifier, OffsetDateTime expiresAt) {
            invitation = new ActivationInvitation(INVITATION_ID, ACCOUNT_ID, verifier, expiresAt);
        }

        @Override
        public Optional<ActivationInvitation> findAvailable(UUID invitationId) {
            lookups++;
            return available ? Optional.of(invitation) : Optional.empty();
        }

        @Override
        public Optional<UUID> accept(
                ActivationInvitation invitation,
                String hash,
                AdultDeclaration adultDeclaration,
                UUID correlationId) {
            passwordHash = hash;
            if (!acceptSucceeds) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString("20000000-0000-0000-0000-000000000003"));
        }
    }

    private static final class EventsFake implements InvitationActivationPublisher {
        private RunnerInvitationActivated event;

        @Override
        public void publish(RunnerInvitationActivated value) {
            event = value;
        }
    }
}
