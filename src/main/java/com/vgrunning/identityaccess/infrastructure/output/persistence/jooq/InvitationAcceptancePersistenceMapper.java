package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import com.vgrunning.identityaccess.application.port.out.InvitationRepository.ActivationInvitation;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.vgrunning.generated.jooq.identity_access.tables.records.AccessChallengeRecord;

/** Traduce el desafío jOOQ bloqueado al contrato mínimo del caso de uso de activación. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvitationAcceptancePersistenceMapper {
    @Mapping(target = "id", source = "id", qualifiedByName = "required")
    @Mapping(target = "accountId", source = "accountId", qualifiedByName = "required")
    @Mapping(target = "verifier", source = "verifierSha256", qualifiedByName = "required")
    @Mapping(target = "expiresAt", source = "expiresAt", qualifiedByName = "required")
    ActivationInvitation toActivationInvitation(AccessChallengeRecord challenge);

    @Named("required")
    static <T> T required(T value) {
        return Objects.requireNonNull(value);
    }
}
