package com.vgrunning.identityaccess.application.service;

import com.vgrunning.identityaccess.application.exception.InvalidInvitationAcceptanceException;
import com.vgrunning.identityaccess.application.exception.InvitationNotAvailableException;
import com.vgrunning.identityaccess.application.mapper.InvitationActivationMapper;
import com.vgrunning.identityaccess.application.port.in.AcceptInvitationUseCase;
import com.vgrunning.identityaccess.application.port.out.DigestPort;
import com.vgrunning.identityaccess.application.port.out.InvitationActivationPublisher;
import com.vgrunning.identityaccess.application.port.out.InvitationRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import java.security.MessageDigest;
import java.text.Normalizer;
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
        if (!MessageDigest.isEqual(invitation.verifier(), digest.sha256(command.secret()))) {
            throw new InvitationNotAvailableException();
        }
        String password = Normalizer.normalize(command.password(), Normalizer.Form.NFC);
        if (password.length() < 12 || password.length() > 128) {
            throw new InvalidInvitationAcceptanceException();
        }
        UUID correlationId = UUID.randomUUID();
        UUID acceptanceId = invitations.accept(invitation, passwords.hash(password), correlationId);
        activationPublisher.publish(mapper.toActivation(invitation, correlationId));
        return new AcceptedInvitation(acceptanceId, "accepted");
    }
}
