package com.vgrunning.identityaccess.infrastructure.input.web;

import com.vgrunning.identityaccess.application.port.in.AcceptInvitationUseCase.AcceptInvitation;
import com.vgrunning.identityaccess.application.port.in.AcceptInvitationUseCase.AcceptedInvitation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.vgrunning.generated.openapi.server.model.InvitationAcceptance;
import org.vgrunning.generated.openapi.server.model.InvitationAcceptanceCreation;

/** Traduce la aceptación HTTP a los contratos internos de identidad. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvitationAcceptanceWebMapper {
    AcceptInvitation toCommand(InvitationAcceptanceCreation request);

    @Mapping(target = "id", source = "acceptanceId")
    InvitationAcceptance toResponse(AcceptedInvitation accepted);

    default InvitationAcceptance.StatusEnum map(String status) {
        return InvitationAcceptance.StatusEnum.fromValue(status);
    }
}
