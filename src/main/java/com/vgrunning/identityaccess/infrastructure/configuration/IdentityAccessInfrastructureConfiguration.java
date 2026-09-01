package com.vgrunning.identityaccess.infrastructure.configuration;

import com.vgrunning.identityaccess.application.SessionService;
import com.vgrunning.identityaccess.application.SyntheticAccountProvisioner;
import com.vgrunning.identityaccess.application.port.out.CurrentTimeProvider;
import com.vgrunning.identityaccess.application.port.out.IdentityAccessRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.port.out.RateLimitKeyDeriver;
import com.vgrunning.identityaccess.application.port.out.SessionTokenService;
import com.vgrunning.identityaccess.domain.securityevent.SecurityEventRetention;
import com.vgrunning.identityaccess.domain.session.SessionSecurityPolicy;
import com.vgrunning.identityaccess.infrastructure.security.Argon2PasswordHasher;
import com.vgrunning.identityaccess.infrastructure.security.CurrentSessionIdentityResolver;
import com.vgrunning.identityaccess.infrastructure.security.HmacRateLimitKeyDeriver;
import com.vgrunning.identityaccess.infrastructure.security.OpaqueSessionSecurityContextRepository;
import com.vgrunning.identityaccess.infrastructure.security.OriginValidationFilter;
import com.vgrunning.identityaccess.infrastructure.security.SecureSessionTokenService;
import com.vgrunning.identityaccess.infrastructure.security.SessionCookieManager;
import com.vgrunning.identityaccess.infrastructure.security.SpringCsrfTokenManager;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Compone los casos de uso con sus adaptadores técnicos sin contaminar la aplicación. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityAccessProperties.class)
public class IdentityAccessInfrastructureConfiguration {

    @Bean
    PasswordHasher identityAccessPasswordHasher() {
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
        return new Argon2PasswordHasher(encoder);
    }

    @Bean
    RateLimitKeyDeriver identityAccessRateLimitKeyDeriver(IdentityAccessProperties properties) {
        return new HmacRateLimitKeyDeriver(properties.rateLimitHmacKey());
    }

    @Bean
    SessionTokenService identityAccessSessionTokenService() {
        return new SecureSessionTokenService();
    }

    @Bean
    SessionSecurityPolicy sessionSecurityPolicy() {
        return new SessionSecurityPolicy(
                Duration.ofHours(12), Duration.ofDays(7), Duration.ofMinutes(15), 5, 20);
    }

    @Bean
    SecurityEventRetention securityEventRetention(IdentityAccessProperties properties) {
        return new SecurityEventRetention(properties.securityEventRetention());
    }

    @Bean
    SessionService sessionService(
            IdentityAccessRepository repository,
            CurrentTimeProvider timeProvider,
            RateLimitKeyDeriver rateLimitKeys,
            PasswordHasher passwordHasher,
            SessionTokenService sessionTokens,
            SessionSecurityPolicy policy) {
        return new SessionService(
                repository, timeProvider, rateLimitKeys, passwordHasher, sessionTokens, policy);
    }

    @Bean
    SyntheticAccountProvisioner syntheticAccountProvisioner(
            IdentityAccessRepository repository,
            CurrentTimeProvider timeProvider,
            PasswordHasher passwordHasher) {
        return new SyntheticAccountProvisioner(repository, timeProvider, passwordHasher);
    }

    @Bean
    SpringCsrfTokenManager springCsrfTokenManager(CsrfTokenRepository repository) {
        return new SpringCsrfTokenManager(repository);
    }

    @Bean
    SessionCookieManager sessionCookieManager() {
        return new SessionCookieManager();
    }

    @Bean
    CurrentSessionIdentityResolver currentSessionIdentityResolver() {
        return new CurrentSessionIdentityResolver();
    }

    @Bean
    SecurityContextRepository opaqueSessionSecurityContextRepository(SessionService sessions) {
        return new OpaqueSessionSecurityContextRepository(sessions);
    }

    @Bean
    OriginValidationFilter originValidationFilter(
            HandlerExceptionResolver handlerExceptionResolver) {
        return new OriginValidationFilter(handlerExceptionResolver);
    }
}
