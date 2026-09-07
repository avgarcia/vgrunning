package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import com.vgrunning.identityaccess.application.port.out.RunnerInvitationProvisioningRepository.PendingRunnerInvitation;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.vgrunning.generated.jooq.identity_access.tables.records.AccessChallengeRecord;
import org.vgrunning.generated.jooq.identity_access.tables.records.AccountEmailRecord;
import org.vgrunning.generated.jooq.identity_access.tables.records.AccountRecord;
import org.vgrunning.generated.jooq.identity_access.tables.records.AdultDeclarationRecord;

/** Traduce los datos de provisión de una invitación a registros jOOQ de identidad. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvitationPersistenceMapper {
    @Mapping(target = "activationExpiresAt", source = "expiresAt")
    ProvisionedRunnerAccount toProvisionedRunnerAccount(
            PendingRunnerInvitation invitation, OffsetDateTime expiresAt);

    @Mapping(target = "id", source = "invitation.accountId")
    @Mapping(target = "role", constant = "corredor")
    @Mapping(target = "status", constant = "pending_activation")
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "statusChangedAt", source = "now")
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "version", constant = "0L")
    AccountRecord toAccountRecord(PendingRunnerInvitation invitation, OffsetDateTime now);

    @Mapping(target = "id", source = "invitation.emailId")
    @Mapping(target = "accountId", source = "invitation.accountId")
    @Mapping(target = "presentationEmail", source = "invitation.presentationEmail")
    @Mapping(target = "canonicalEmail", source = "invitation.canonicalEmail")
    @Mapping(target = "usage", constant = "current")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "releasedAt", ignore = true)
    AccountEmailRecord toAccountEmailRecord(PendingRunnerInvitation invitation, OffsetDateTime now);

    @Mapping(target = "id", source = "invitation.invitationId")
    @Mapping(target = "accountId", source = "invitation.accountId")
    @Mapping(target = "purpose", constant = "activation")
    @Mapping(target = "generation", constant = "1")
    @Mapping(
            target = "verifierSha256",
            source = "invitation.secretVerifier",
            qualifiedByName = "required")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "consumedAt", ignore = true)
    @Mapping(target = "replacedAt", ignore = true)
    AccessChallengeRecord toAccessChallengeRecord(
            PendingRunnerInvitation invitation, OffsetDateTime now, OffsetDateTime expiresAt);

    @Mapping(target = "id", source = "invitation.declarationId")
    @Mapping(target = "accountId", source = "invitation.accountId")
    @Mapping(target = "actorKind", constant = "administrator")
    @Mapping(target = "actorAccountId", source = "invitation.administratorAccountId")
    @Mapping(target = "origin", constant = "administrative_invitation")
    @Mapping(target = "declaredAt", source = "now")
    @Mapping(target = "textVersion", constant = "ux-02-v0.1")
    AdultDeclarationRecord toAdministratorDeclaration(
            PendingRunnerInvitation invitation, OffsetDateTime now);

    @org.mapstruct.Named("required")
    static <T> T required(T value) {
        return java.util.Objects.requireNonNull(value);
    }
}
