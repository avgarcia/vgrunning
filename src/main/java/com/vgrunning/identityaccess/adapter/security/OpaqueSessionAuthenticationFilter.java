package com.vgrunning.identityaccess.adapter.security;

import com.vgrunning.identityaccess.application.AuthenticatedSession;
import com.vgrunning.identityaccess.application.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Convierte una cookie opaca válida en una identidad interna por petición, sin sesión HTTP. */
@Component
public class OpaqueSessionAuthenticationFilter extends OncePerRequestFilter {
    public static final String SESSION_COOKIE = "__Host-pmv_session";
    private final SessionService sessions;

    public OpaqueSessionAuthenticationFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        findCookie(request, SESSION_COOKIE)
                .flatMap(sessions::authenticate)
                .ifPresent(session -> authenticateRequest(session));
        filterChain.doFilter(request, response);
    }

    private static Optional<String> findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private static void authenticateRequest(AuthenticatedSession session) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        session,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_"
                                                + session.role()
                                                        .toUpperCase(java.util.Locale.ROOT)))));
        SecurityContextHolder.setContext(context);
    }
}
