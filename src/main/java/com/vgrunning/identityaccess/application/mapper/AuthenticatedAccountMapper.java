package com.vgrunning.identityaccess.application.mapper;

import com.vgrunning.identityaccess.application.port.in.AuthenticateCredentialsUseCase.AuthenticatedAccount;
import com.vgrunning.identityaccess.application.port.out.AccountRepository.CredentialAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Convierte el resultado de persistencia en el resultado local del caso de uso. */
@Mapper
public interface AuthenticatedAccountMapper {
    @Mapping(target = "accountId", source = "id")
    AuthenticatedAccount toAuthenticatedAccount(CredentialAccount credentialAccount);
}
