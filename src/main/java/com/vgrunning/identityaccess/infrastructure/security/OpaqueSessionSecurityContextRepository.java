package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.application.model.SessionIdentity;
import com.vgrunning.identityaccess.application.port.in.ResolveSessionUseCase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/** Resuelve la cookie opaca mediante la aplicación sin utilizar una sesión HTTP de Spring. */
public final class OpaqueSessionSecurityContextRepository implements SecurityContextRepository {
    private final ResolveSessionUseCase sessions;

    public OpaqueSessionSecurityContextRepository(ResolveSessionUseCase sessions) {
        this.sessions = sessions;
    }

    @Override
    @SuppressWarnings("deprecation")
    public SecurityContext loadContext(HttpRequestResponseHolder holder) {
        return findSessionCookie(holder.getRequest())
                .flatMap(sessions::resolve)
                .map(OpaqueSessionSecurityContextRepository::authenticatedContext)
                .orElseGet(SecurityContextHolder::createEmptyContext);
    }

    @Override
    public void saveContext(
            SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        // La sesión opaca se crea y revoca mediante casos de uso explícitos.
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return findSessionCookie(request).isPresent();
    }

    private static Optional<String> findSessionCookie(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies()).stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> SessionCookieManager.SESSION_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private static SecurityContext authenticatedContext(SessionIdentity session) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        session,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_"
                                                + session.role()
                                                        .value()
                                                        .toUpperCase(Locale.ROOT)))));
        return context;
    }
}
