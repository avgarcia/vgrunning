package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import com.vgrunning.identityaccess.infrastructure.security.session.SessionPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/** Adapta el principal técnico persistido por Spring Session al actor explícito del caso de uso. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SessionActorMapper {
    @Mapping(target = "role", source = "role")
    ActorContext toActorContext(SessionPrincipal principal);

    default String map(com.vgrunning.identityaccess.domain.account.valueobject.AccountRole role) {
        return role.value();
    }
}
