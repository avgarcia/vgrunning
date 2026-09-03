package com.vgrunning.identityaccess.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Rechaza orígenes declarados que no correspondan al host directo de la solicitud. */
@RequiredArgsConstructor
public final class OriginValidationFilter extends OncePerRequestFilter {
    private final HandlerExceptionResolver handlerExceptionResolver;

    /** Omite los métodos seguros, que no requieren protección CSRF ni validación de origen. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                && !HttpMethod.DELETE.matches(request.getMethod());
    }

    /** Rechaza un encabezado Origin presente que no represente el mismo origen de la petición. */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && !matchesRequestOrigin(origin, request)) {
            handlerExceptionResolver.resolveException(
                    request, response, null, new CsrfValidationException());
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Compara el Origin sintácticamente válido con esquema, host y puerto de la petición. */
    private static boolean matchesRequestOrigin(String origin, HttpServletRequest request) {
        try {
            URI parsed = new URI(origin);
            String scheme = parsed.getScheme();
            String host = parsed.getHost();
            if (parsed.isOpaque()
                    || scheme == null
                    || host == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return false;
            }
            int expectedPort = request.getServerPort();
            int actualPort = parsed.getPort() == -1 ? defaultPort(scheme) : parsed.getPort();
            return parsed.getUserInfo() == null
                    && parsed.getPath().isEmpty()
                    && parsed.getQuery() == null
                    && parsed.getFragment() == null
                    && request.getScheme().equalsIgnoreCase(scheme)
                    && request.getServerName().equalsIgnoreCase(host)
                    && expectedPort == actualPort;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    /** Resuelve el puerto efectivo cuando el Origin utiliza el puerto estándar de su esquema. */
    private static int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }
}
