package com.vgrunning.identityaccess.adapter.in.web;

import com.vgrunning.identityaccess.application.model.SessionLogin;
import com.vgrunning.identityaccess.application.port.in.CreateSessionUseCase;
import com.vgrunning.identityaccess.application.port.in.RevokeSessionUseCase;
import com.vgrunning.identityaccess.infrastructure.security.CsrfTokenRotator;
import com.vgrunning.identityaccess.infrastructure.security.CurrentSessionIdentityResolver;
import com.vgrunning.identityaccess.infrastructure.security.SessionCookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.vgrunning.generated.openapi.server.api.SessionsApi;
import org.vgrunning.generated.openapi.server.model.CurrentSession;
import org.vgrunning.generated.openapi.server.model.SessionCreation;

/** Implementa el contrato HTTP de sesiones y delega la seguridad técnica en infraestructura. */
@RestController
@RequiredArgsConstructor
public class SessionHttpController implements SessionsApi {
    private final CreateSessionUseCase createSession;
    private final RevokeSessionUseCase revokeSession;
    private final SessionCookieManager sessionCookies;
    private final CsrfTokenRotator csrfTokens;
    private final CurrentSessionIdentityResolver currentSession;
    private final SessionWebMapper mapper;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Override
    public ResponseEntity<CurrentSession> createSession(SessionCreation sessionCreation) {
        SessionLogin login =
                createSession.create(
                        sessionCreation.getEmail(),
                        sessionCreation.getPassword(),
                        request.getRemoteAddr());
        sessionCookies.write(login.rawSessionToken(), response);
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
        revokeSession.revoke(currentSession.current());
        sessionCookies.expire(response);
        csrfTokens.rotate(request, response);
        return ResponseEntity.noContent().build();
    }
}
