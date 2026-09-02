package com.vgrunning.identityaccess.adapter.in.web;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.exception.RateLimitedException;
import com.vgrunning.identityaccess.application.model.SessionLogin;
import com.vgrunning.identityaccess.application.port.in.CreateSessionUseCase;
import com.vgrunning.identityaccess.infrastructure.security.CsrfTokenRotator;
import com.vgrunning.identityaccess.infrastructure.security.CurrentSessionIdentityResolver;
import com.vgrunning.identityaccess.infrastructure.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.RestController;
import org.vgrunning.generated.openapi.server.api.SessionsApi;
import org.vgrunning.generated.openapi.server.model.CurrentSession;
import org.vgrunning.generated.openapi.server.model.SessionCreation;

/** Implementa el contrato HTTP de sesiones y delega la seguridad técnica en infraestructura. */
@RestController
@RequiredArgsConstructor
public class SessionHttpController implements SessionsApi {
    private final CreateSessionUseCase createSession;
    private final LoginRateLimiter rateLimiter;
    private final SecurityContextRepository securityContexts;
    private final CsrfTokenRotator csrfTokens;
    private final CurrentSessionIdentityResolver currentSession;
    private final SessionWebMapper mapper;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Override
    public ResponseEntity<CurrentSession> createSession(SessionCreation sessionCreation) {
        if (rateLimiter.limited(sessionCreation.getEmail(), request.getRemoteAddr())) {
            throw new RateLimitedException(Duration.ofMinutes(15));
        }
        SessionLogin login;
        try {
            login = createSession.create(sessionCreation.getEmail(), sessionCreation.getPassword());
        } catch (InvalidCredentialsException exception) {
            if (rateLimiter.registerFailure(sessionCreation.getEmail(), request.getRemoteAddr())) {
                throw new RateLimitedException(Duration.ofMinutes(15));
            }
            throw exception;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority(
                        "ROLE_" + login.session().role().value().toUpperCase(Locale.ROOT));
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        login.session(), null, List.of(authority)));
        SecurityContextHolder.setContext(context);
        securityContexts.saveContext(context, request, response);
        csrfTokens.rotate(request, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/sessions/current")
                .body(mapper.toResponse(login.session()));
    }

    @Override
    public ResponseEntity<CurrentSession> getCurrentSession() {
        return ResponseEntity.ok(mapper.toResponse(currentSession.current()));
    }

    @Override
    public ResponseEntity<Void> deleteCurrentSession() {
        currentSession.current();
        Objects.requireNonNull(request.getSession(false)).invalidate();
        csrfTokens.rotate(request, response);
        return ResponseEntity.noContent().build();
    }
}
