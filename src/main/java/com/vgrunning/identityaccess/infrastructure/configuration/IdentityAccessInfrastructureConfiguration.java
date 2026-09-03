package com.vgrunning.identityaccess.infrastructure.configuration;

import com.vgrunning.identityaccess.application.mapper.AuthenticatedAccountMapper;
import com.vgrunning.identityaccess.application.port.in.AuthenticateCredentialsUseCase;
import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.service.AuthenticateCredentialsService;
import com.vgrunning.identityaccess.infrastructure.security.Argon2PasswordHasher;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/** Compone el caso de uso de autenticación con sus dependencias técnicas inmediatas. */
@Configuration(proxyBeanMethods = false)
public class IdentityAccessInfrastructureConfiguration {

    /** Crea el hasher Argon2id con los parámetros aprobados para credenciales locales. */
    @Bean
    PasswordHasher identityAccessPasswordHasher() {
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
        return new Argon2PasswordHasher(encoder);
    }

    /** Compone el caso de uso de autenticación sin exponer infraestructura a aplicación. */
    @Bean
    AuthenticateCredentialsUseCase authenticateCredentialsUseCase(
            AccountRepository accounts,
            PasswordHasher passwordHasher,
            AuthenticatedAccountMapper authenticatedAccountMapper) {
        return new AuthenticateCredentialsService(
                accounts, passwordHasher, authenticatedAccountMapper);
    }

    /** Obtiene la implementación MapStruct generada para el contrato interno de autenticación. */
    @Bean
    AuthenticatedAccountMapper authenticatedAccountMapper() {
        return Mappers.getMapper(AuthenticatedAccountMapper.class);
    }
}
