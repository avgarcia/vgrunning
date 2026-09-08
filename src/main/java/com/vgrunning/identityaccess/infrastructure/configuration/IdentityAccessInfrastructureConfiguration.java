package com.vgrunning.identityaccess.infrastructure.configuration;

import com.vgrunning.identityaccess.api.provisioning.AccountProvisioningApi;
import com.vgrunning.identityaccess.application.mapper.AuthenticatedAccountMapper;
import com.vgrunning.identityaccess.application.mapper.InvitationActivationMapper;
import com.vgrunning.identityaccess.application.port.in.AcceptInvitationUseCase;
import com.vgrunning.identityaccess.application.port.in.AuthenticateCredentialsUseCase;
import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import com.vgrunning.identityaccess.application.port.out.ActivationLinkFactory;
import com.vgrunning.identityaccess.application.port.out.DigestPort;
import com.vgrunning.identityaccess.application.port.out.InvitationActivationPublisher;
import com.vgrunning.identityaccess.application.port.out.InvitationPayloadProtector;
import com.vgrunning.identityaccess.application.port.out.InvitationRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.port.out.RunnerInvitationProvisioningRepository;
import com.vgrunning.identityaccess.application.port.out.SecretGenerator;
import com.vgrunning.identityaccess.application.service.AcceptInvitationService;
import com.vgrunning.identityaccess.application.service.AuthenticateCredentialsService;
import com.vgrunning.identityaccess.application.service.ProvisionRunnerAccountService;
import com.vgrunning.identityaccess.infrastructure.output.event.SpringInvitationActivationPublisher;
import com.vgrunning.identityaccess.infrastructure.output.provider.ActivationRouteLinkFactory;
import com.vgrunning.identityaccess.infrastructure.security.AesGcmInvitationPayloadProtector;
import com.vgrunning.identityaccess.infrastructure.security.Argon2PasswordHasher;
import com.vgrunning.identityaccess.infrastructure.security.SecureRandomSecretGenerator;
import com.vgrunning.identityaccess.infrastructure.security.Sha256DigestAdapter;
import com.vgrunning.notificationdelivery.api.request.NotificationRequestApi;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/** Compone el caso de uso de autenticación con sus dependencias técnicas inmediatas. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InvitationEncryptionProperties.class)
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

    @Bean
    InvitationPayloadProtector invitationPayloadProtector(
            InvitationEncryptionProperties properties) {
        if (properties.key() == null || properties.key().isBlank()) {
            throw new IllegalStateException("Falta PMV_IDENTITY_INVITATION_ENCRYPTION_KEY.");
        }
        return new AesGcmInvitationPayloadProtector(properties.key());
    }

    @Bean
    DigestPort identityAccessDigestPort() {
        return new Sha256DigestAdapter();
    }

    @Bean
    SecretGenerator identityAccessSecretGenerator() {
        return new SecureRandomSecretGenerator();
    }

    @Bean
    ActivationLinkFactory activationLinkFactory() {
        return new ActivationRouteLinkFactory();
    }

    @Bean
    AccountProvisioningApi accountProvisioningApi(
            RunnerInvitationProvisioningRepository invitations,
            InvitationPayloadProtector protector,
            NotificationRequestApi notifications,
            DigestPort digest,
            SecretGenerator secrets,
            ActivationLinkFactory activationLinks) {
        return new ProvisionRunnerAccountService(
                invitations, protector, notifications, digest, secrets, activationLinks);
    }

    @Bean
    AcceptInvitationUseCase acceptInvitationUseCase(
            InvitationRepository invitations,
            InvitationActivationPublisher activationPublisher,
            PasswordHasher passwordHasher,
            InvitationActivationMapper invitationActivationMapper,
            DigestPort digest) {
        return new AcceptInvitationService(
                invitations,
                activationPublisher,
                passwordHasher,
                invitationActivationMapper,
                digest);
    }

    @Bean
    InvitationActivationPublisher invitationActivationPublisher(ApplicationEventPublisher events) {
        return new SpringInvitationActivationPublisher(events);
    }

    @Bean
    InvitationActivationMapper invitationActivationMapper() {
        return Mappers.getMapper(InvitationActivationMapper.class);
    }
}
