package com.vgrunning.identityaccess.infrastructure.input.web;

import com.vgrunning.identityaccess.application.port.in.AuthenticateCredentialsUseCase;
import com.vgrunning.identityaccess.domain.account.valueobject.EmailAddress;
import com.vgrunning.identityaccess.infrastructure.security.CurrentSessionIdentityResolver;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.LoginRateLimiter;
import com.vgrunning.identityaccess.infrastructure.security.session.SessionPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.RestController;
import org.vgrunning.generated.openapi.server.api.SessionsApi;
import org.vgrunning.generated.openapi.server.model.CurrentSession;
import org.vgrunning.generated.openapi.server.model.SessionCreation;

/** Implementa el contrato HTTP de sesiones y delega la seguridad técnica en infraestructura. */
@RestController
@RequiredArgsConstructor
public class SessionHttpController implements SessionsApi {
    private final AuthenticateCredentialsUseCase authenticateCredentials;
    private final LoginRateLimiter rateLimiter;
    private final SecurityContextRepository securityContexts;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final LogoutHandler sessionLogoutHandler;
    private final CurrentSessionIdentityResolver currentSession;
    private final SessionWebMapper mapper;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /** Autentica credenciales y crea el contexto técnico que Spring Session persiste. */
    @Override
    public ResponseEntity<CurrentSession> createSession(SessionCreation sessionCreation) {
        String canonicalEmail = EmailAddress.canonicalize(sessionCreation.getEmail());
        rateLimiter.checkAndConsume(canonicalEmail, request.getRemoteAddr());
        AuthenticateCredentialsUseCase.AuthenticatedAccount authenticatedAccount =
                authenticateCredentials.authenticate(
                        sessionCreation.getEmail(), sessionCreation.getPassword());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority(
                        "ROLE_" + authenticatedAccount.role().value().toUpperCase(Locale.ROOT));
        SessionPrincipal principal = mapper.toPrincipal(authenticatedAccount);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of(authority));
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContexts.saveContext(context, request, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/sessions/current")
                .body(mapper.toResponse(principal));
    }

    /** Devuelve la identidad almacenada en la sesión HTTP vigente. */
    @Override
    public ResponseEntity<CurrentSession> getCurrentSession() {
        return ResponseEntity.ok(mapper.toResponse(currentSession.current()));
    }

    /** Invalida la sesión HTTP vigente sin exponer ni gestionar su identificador. */
    @Override
    public ResponseEntity<Void> deleteCurrentSession() {
        currentSession.current();
        sessionLogoutHandler.logout(
                request, response, SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.noContent().build();
    }
}
