package com.vgrunning.identityaccess.infrastructure.output.persistence.jooq;

import com.vgrunning.identityaccess.application.port.out.AccountRepository.CredentialAccount;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.vgrunning.generated.jooq.identity_access.tables.records.AccountEmailRecord;
import org.vgrunning.generated.jooq.identity_access.tables.records.AccountRecord;

/** Traduce exclusivamente las representaciones jOOQ de identidad. */
@Mapper(componentModel = "spring")
public interface AccountPersistenceMapper {
    CredentialAccount toCredentialAccount(AccountRecord accountRecord);

    @Mapping(target = "id", source = "accountId")
    @Mapping(target = "status", constant = "active")
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "statusChangedAt", source = "now")
    @Mapping(target = "passwordChangedAt", source = "now")
    AccountRecord toAccountRecord(
            JooqSyntheticAccountRepository.SyntheticAccountProvision provision);

    @Mapping(target = "id", source = "emailId")
    @Mapping(target = "accountId", source = "provision.accountId")
    @Mapping(target = "presentationEmail", source = "provision.presentationEmail")
    @Mapping(target = "canonicalEmail", source = "provision.canonicalEmail")
    @Mapping(target = "usage", constant = "current")
    @Mapping(target = "createdAt", source = "provision.now")
    @Mapping(target = "updatedAt", source = "provision.now")
    @Mapping(target = "confirmedAt", source = "provision.now")
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "releasedAt", ignore = true)
    AccountEmailRecord toAccountEmailRecord(
            JooqSyntheticAccountRepository.SyntheticAccountProvision provision, UUID emailId);

    default AccountRole toAccountRole(String role) {
        return AccountRole.fromValue(role);
    }

    default AccountStatus toAccountStatus(String status) {
        return AccountStatus.fromValue(status);
    }

    default String toPersistenceRole(AccountRole role) {
        return role.value();
    }
}
