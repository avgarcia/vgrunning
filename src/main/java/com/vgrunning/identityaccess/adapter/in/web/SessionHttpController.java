package com.vgrunning.identityaccess.adapter.in.web;

import com.vgrunning.identityaccess.application.SessionIdentity;
import com.vgrunning.identityaccess.application.SessionLogin;
import com.vgrunning.identityaccess.application.SessionService;
import com.vgrunning.identityaccess.infrastructure.security.CurrentSessionIdentityResolver;
import com.vgrunning.identityaccess.infrastructure.security.SessionCookieManager;
import com.vgrunning.identityaccess.infrastructure.security.SpringCsrfTokenManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.vgrunning.generated.openapi.server.api.SessionsApi;
import org.vgrunning.generated.openapi.server.model.AccountRole;
import org.vgrunning.generated.openapi.server.model.AccountStatus;
import org.vgrunning.generated.openapi.server.model.CurrentSession;
import org.vgrunning.generated.openapi.server.model.SessionCreation;

/** Implementa el contrato HTTP de sesiones y delega la seguridad técnica en infraestructura. */
@RestController
@RequiredArgsConstructor
public class SessionHttpController implements SessionsApi {
    private final SessionService sessions;
    private final SessionCookieManager sessionCookies;
    private final SpringCsrfTokenManager csrfTokens;
    private final CurrentSessionIdentityResolver currentSession;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Override
    public ResponseEntity<CurrentSession> createSession(SessionCreation sessionCreation) {
        SessionLogin login =
                sessions.login(
                        sessionCreation.getEmail(),
                        sessionCreation.getPassword(),
                        request.getRemoteAddr());
        sessionCookies.write(login.rawSessionToken(), response);
        csrfTokens.rotate(request, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/sessions/current")
                .body(toCurrentSession(login.session()));
    }

    @Override
    public ResponseEntity<CurrentSession> getCurrentSession() {
        return ResponseEntity.ok(toCurrentSession(currentSession.current()));
    }

    @Override
    public ResponseEntity<Void> deleteCurrentSession() {
        sessions.logout(currentSession.current());
        sessionCookies.expire(response);
        csrfTokens.rotate(request, response);
        return ResponseEntity.noContent().build();
    }

    private static CurrentSession toCurrentSession(SessionIdentity session) {
        return new CurrentSession(
                session.accountId(),
                AccountRole.fromValue(session.role().value()),
                AccountStatus.fromValue(session.status().value()));
    }
}
