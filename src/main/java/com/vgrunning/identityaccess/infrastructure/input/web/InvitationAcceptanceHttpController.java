package com.vgrunning.identityaccess.infrastructure.input.web;

import com.vgrunning.identityaccess.application.port.in.AcceptInvitationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.vgrunning.generated.openapi.server.api.InvitationAcceptancesApi;
import org.vgrunning.generated.openapi.server.model.InvitationAcceptance;
import org.vgrunning.generated.openapi.server.model.InvitationAcceptanceCreation;

/** Entrada anónima que consume una invitación vigente sin crear sesión automáticamente. */
@RestController
@RequiredArgsConstructor
public class InvitationAcceptanceHttpController implements InvitationAcceptancesApi {
    private final AcceptInvitationUseCase acceptInvitation;
    private final InvitationAcceptanceWebMapper mapper;

    @Override
    public ResponseEntity<InvitationAcceptance> createInvitationAcceptance(
            InvitationAcceptanceCreation invitationAcceptanceCreation) {
        var accepted = acceptInvitation.accept(mapper.toCommand(invitationAcceptanceCreation));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(
                        HttpHeaders.LOCATION,
                        "/api/invitation-acceptances/" + accepted.acceptanceId())
                .body(mapper.toResponse(accepted));
    }
}
