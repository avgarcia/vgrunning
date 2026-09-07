package com.vgrunning.identityaccess.application.mapper;

import com.vgrunning.identityaccess.api.provisioning.RunnerInvitationActivated;
import com.vgrunning.identityaccess.application.port.out.InvitationRepository.ActivationInvitation;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Convierte una invitación aceptada en el hecho público que activa al corredor. */
@Mapper
public interface InvitationActivationMapper {
    @Mapping(target = "accountId", source = "invitation.accountId")
    RunnerInvitationActivated toActivation(ActivationInvitation invitation, UUID correlationId);
}
