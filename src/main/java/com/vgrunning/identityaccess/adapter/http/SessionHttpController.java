package com.vgrunning.identityaccess.adapter.http;

import com.vgrunning.identityaccess.adapter.security.OpaqueSessionAuthenticationFilter;
import com.vgrunning.identityaccess.application.AuthenticatedSession;
import com.vgrunning.identityaccess.application.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.RateLimitedException;
import com.vgrunning.identityaccess.application.SessionLogin;
import com.vgrunning.identityaccess.application.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.vgrunning.generated.openapi.server.api.CsrfTokensApi;
import org.vgrunning.generated.openapi.server.api.SessionsApi;
import org.vgrunning.generated.openapi.server.model.AccountRole;
import org.vgrunning.generated.openapi.server.model.AccountStatus;
import org.vgrunning.generated.openapi.server.model.CurrentSession;
import org.vgrunning.generated.openapi.server.model.SessionCreation;

/** Adaptador HTTP que implementa los contratos OpenAPI de sesión sin exponer secretos. */
@RestController
@RequiredArgsConstructor
public class SessionHttpController implements CsrfTokensApi, SessionsApi {
    private final SessionService sessions;
    private final CsrfTokenRepository csrfTokens;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    @Override
    public ResponseEntity<org.vgrunning.generated.openapi.server.model.CsrfToken>
            getCurrentCsrfToken() {
        CsrfToken token = csrfTokens.loadToken(request);
        if (token == null) {
            token = csrfTokens.generateToken(request);
            csrfTokens.saveToken(token, request, response);
        }
        return ResponseEntity.ok(
                new org.vgrunning.generated.openapi.server.model.CsrfToken(token.getToken()));
    }

    @Override
    public ResponseEntity<CurrentSession> createSession(SessionCreation sessionCreation) {
        SessionLogin login =
                sessions.login(
                        sessionCreation.getEmail(),
                        sessionCreation.getPassword(),
                        request.getRemoteAddr());
        response.addHeader(
                HttpHeaders.SET_COOKIE, sessionCookie(login.rawSessionToken()).toString());
        rotateCsrfToken();
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/sessions/current")
                .body(toCurrentSession(login.session()));
    }

    @Override
    public ResponseEntity<CurrentSession> getCurrentSession() {
        return ResponseEntity.ok(toCurrentSession(currentAuthentication()));
    }

    @Override
    public ResponseEntity<Void> deleteCurrentSession() {
        sessions.logout(currentAuthentication());
        response.addHeader(HttpHeaders.SET_COOKIE, expiredSessionCookie().toString());
        rotateCsrfToken();
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedSession currentAuthentication() {
        Authentication authentication =
                Optional.ofNullable(
                                org.springframework.security.core.context.SecurityContextHolder
                                        .getContext()
                                        .getAuthentication())
                        .orElseThrow(IllegalStateException::new);
        return (AuthenticatedSession) authentication.getPrincipal();
    }

    private void rotateCsrfToken() {
        csrfTokens.saveToken(null, request, response);
        CsrfToken replacement = csrfTokens.generateToken(request);
        csrfTokens.saveToken(replacement, request, response);
    }

    private static CurrentSession toCurrentSession(AuthenticatedSession session) {
        return new CurrentSession(
                session.accountId(),
                AccountRole.fromValue(session.role()),
                AccountStatus.fromValue(session.status()));
    }

    private static ResponseCookie sessionCookie(String rawToken) {
        return ResponseCookie.from(OpaqueSessionAuthenticationFilter.SESSION_COOKIE, rawToken)
                .secure(true)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .build();
    }

    private static ResponseCookie expiredSessionCookie() {
        return ResponseCookie.from(OpaqueSessionAuthenticationFilter.SESSION_COOKIE, "")
                .secure(true)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

}
