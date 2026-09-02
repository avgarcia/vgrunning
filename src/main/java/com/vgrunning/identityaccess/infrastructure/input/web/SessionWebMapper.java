package com.vgrunning.identityaccess.infrastructure.input.web;

import com.vgrunning.identityaccess.infrastructure.security.session.SessionPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.vgrunning.generated.openapi.server.model.AccountRole;
import org.vgrunning.generated.openapi.server.model.AccountStatus;
import org.vgrunning.generated.openapi.server.model.CurrentSession;

/** Traduce la identidad técnica a la representación generada por OpenAPI. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SessionWebMapper {
    @Mapping(target = "accountStatus", source = "status")
    CurrentSession toResponse(SessionPrincipal session);

    default AccountRole map(com.vgrunning.identityaccess.domain.account.valueobject.AccountRole role) {
        return AccountRole.fromValue(role.value());
    }

    default AccountStatus map(com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus status) {
        return AccountStatus.fromValue(status.value());
    }
}
