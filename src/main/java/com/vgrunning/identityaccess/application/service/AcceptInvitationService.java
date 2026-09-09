package com.vgrunning.identityaccess.application.service;

import com.vgrunning.identityaccess.application.exception.InvalidInvitationAcceptanceException;
import com.vgrunning.identityaccess.application.exception.InvitationNotAvailableException;
import com.vgrunning.identityaccess.application.mapper.InvitationActivationMapper;
import com.vgrunning.identityaccess.application.port.in.AcceptInvitationUseCase;
import com.vgrunning.identityaccess.application.port.out.DigestPort;
import com.vgrunning.identityaccess.application.port.out.InvitationActivationPublisher;
import com.vgrunning.identityaccess.application.port.out.InvitationRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.domain.AdultDeclaration;
import com.vgrunning.identityaccess.domain.account.valueobject.RawPassword;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Comprueba un desafío de activación y aplica todos sus cambios de estado juntos. */
@RequiredArgsConstructor
public class AcceptInvitationService implements AcceptInvitationUseCase {
    private final InvitationRepository invitations;
    private final InvitationActivationPublisher activationPublisher;
    private final PasswordHasher passwords;
    private final InvitationActivationMapper mapper;
    private final DigestPort digest;
    private final Clock clock;

    @Override
    @Transactional
    public AcceptedInvitation accept(AcceptInvitation command) {
        if (command.invitationId() == null
                || command.secret() == null
                || command.password() == null
                || !Boolean.TRUE.equals(command.adultDeclarationConfirmed())) {
            throw new InvalidInvitationAcceptanceException();
        }
        InvitationRepository.ActivationInvitation invitation =
                invitations
                        .findAvailable(command.invitationId())
                        .orElseThrow(InvitationNotAvailableException::new);
        if (invitation.expiresAt().isBefore(OffsetDateTime.now(clock))) {
            throw new InvitationNotAvailableException();
        }
        if (!MessageDigest.isEqual(invitation.verifier(), digest.sha256(command.secret()))) {
            throw new InvitationNotAvailableException();
        }
        RawPassword password;
        try {
            password = RawPassword.from(command.password());
        } catch (IllegalArgumentException exception) {
            throw new InvalidInvitationAcceptanceException();
        }
        UUID correlationId = UUID.randomUUID();
        UUID acceptanceId =
                invitations
                        .accept(
                                invitation,
                                passwords.hash(password.value()),
                                AdultDeclaration.initialActivation(),
                                correlationId)
                        .orElseThrow(InvitationNotAvailableException::new);
        activationPublisher.publish(mapper.toActivation(invitation, correlationId));
        return new AcceptedInvitation(acceptanceId, "accepted");
    }
}
