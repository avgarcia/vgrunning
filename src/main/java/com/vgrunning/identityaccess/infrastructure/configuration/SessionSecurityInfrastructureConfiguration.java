package com.vgrunning.identityaccess.infrastructure.configuration;

import com.vgrunning.identityaccess.infrastructure.security.CurrentSessionIdentityResolver;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/** Compone Spring Session y los componentes de seguridad asociados al ciclo de sesión HTTP. */
@Configuration(proxyBeanMethods = false)
public class SessionSecurityInfrastructureConfiguration {

    /** Guarda el contexto de Spring Security en la sesión HTTP gestionada por Spring Session. */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** Renueva el identificador de sesión y el token CSRF tras una autenticación satisfactoria. */
    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokens) {
        CsrfAuthenticationStrategy csrf = new CsrfAuthenticationStrategy(csrfTokens);
        csrf.setRequestHandler(
                (request, response, token) ->
                        request.setAttribute(CsrfToken.class.getName(), token.get()));
        return new CompositeSessionAuthenticationStrategy(
                List.of(new ChangeSessionIdAuthenticationStrategy(), csrf));
    }

    /** Invalida el contexto de sesión y emite un token CSRF nuevo al cerrar sesión. */
    @Bean
    LogoutHandler sessionLogoutHandler(
            CsrfTokenRepository csrfTokens, SecurityContextRepository securityContexts) {
        SecurityContextLogoutHandler securityContext = new SecurityContextLogoutHandler();
        securityContext.setSecurityContextRepository(securityContexts);
        return new CompositeLogoutHandler(
                securityContext,
                new CsrfLogoutHandler(csrfTokens),
                (request, response, authentication) ->
                        csrfTokens.saveToken(csrfTokens.generateToken(request), request, response));
    }

    /** Extrae el principal técnico de la sesión vigente para las operaciones autenticadas. */
    @Bean
    CurrentSessionIdentityResolver currentSessionIdentityResolver() {
        return new CurrentSessionIdentityResolver();
    }
}
