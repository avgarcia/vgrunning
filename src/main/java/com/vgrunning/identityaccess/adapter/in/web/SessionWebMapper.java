package com.vgrunning.identityaccess.adapter.in.web;

import com.vgrunning.identityaccess.application.model.SessionIdentity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.vgrunning.generated.openapi.server.model.AccountRole;
import org.vgrunning.generated.openapi.server.model.AccountStatus;
import org.vgrunning.generated.openapi.server.model.CurrentSession;

/** Traduce el modelo de aplicación a la representación generada por OpenAPI. */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SessionWebMapper {
    @Mapping(target = "accountStatus", source = "status")
    CurrentSession toResponse(SessionIdentity session);

    default AccountRole map(com.vgrunning.identityaccess.domain.account.AccountRole role) {
        return AccountRole.fromValue(role.value());
    }

    default AccountStatus map(com.vgrunning.identityaccess.domain.account.AccountStatus status) {
        return AccountStatus.fromValue(status.value());
    }
}
