package com.vgrunning.identityaccess.infrastructure.configuration;

import com.vgrunning.identityaccess.application.port.in.AuthenticateCredentialsUseCase;
import com.vgrunning.identityaccess.application.port.out.AccountRepository;
import com.vgrunning.identityaccess.application.port.out.PasswordHasher;
import com.vgrunning.identityaccess.application.service.AuthenticateCredentialsService;
import com.vgrunning.identityaccess.infrastructure.security.Argon2PasswordHasher;
import com.vgrunning.identityaccess.infrastructure.security.CurrentSessionIdentityResolver;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.LoginRateLimiter;
import com.vgrunning.identityaccess.infrastructure.security.OriginValidationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Compone los casos de uso con sus componentes técnicos sin contaminar la aplicación. */
@Configuration(proxyBeanMethods = false)
public class IdentityAccessInfrastructureConfiguration {

    @Bean
    PasswordHasher identityAccessPasswordHasher() {
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
        return new Argon2PasswordHasher(encoder);
    }

    @Bean
    AuthenticateCredentialsUseCase authenticateCredentialsUseCase(
            AccountRepository accounts, PasswordHasher passwordHasher) {
        return new AuthenticateCredentialsService(accounts, passwordHasher);
    }

    @Bean
    LoginRateLimiter loginRateLimiter() {
        return new LoginRateLimiter();
    }

    @Bean
    CurrentSessionIdentityResolver currentSessionIdentityResolver() {
        return new CurrentSessionIdentityResolver();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CookieSerializer springSessionCookieSerializer() {
        DefaultCookieSerializer cookies = new DefaultCookieSerializer();
        cookies.setCookieName("__Host-pmv_session");
        cookies.setCookiePath("/");
        cookies.setUseSecureCookie(true);
        cookies.setUseHttpOnlyCookie(true);
        cookies.setSameSite("Lax");
        return cookies;
    }

    @Bean
    OriginValidationFilter originValidationFilter(
            HandlerExceptionResolver handlerExceptionResolver) {
        return new OriginValidationFilter(handlerExceptionResolver);
    }
}
