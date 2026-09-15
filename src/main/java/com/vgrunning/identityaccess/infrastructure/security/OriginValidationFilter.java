package com.vgrunning.identityaccess.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Rechaza orígenes declarados que no coincidan con el origen público canónico configurado.
 *
 * <p>No se lee {@code HttpServletRequest#getScheme()/getServerName()/getServerPort()}: detrás de un
 * proxy TLS-terminating como Azure App Service esos valores describen la conexión interna (HTTP,
 * host y puerto del contenedor), no el origen público que el navegador incluye en {@code Origin}.
 * Comparar contra ellos rechazaría toda mutación en producción.
 */
public final class OriginValidationFilter extends OncePerRequestFilter {
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final String canonicalScheme;
    private final String canonicalHost;
    private final int canonicalPort;

    public OriginValidationFilter(
            HandlerExceptionResolver handlerExceptionResolver, String publicOrigin) {
        this.handlerExceptionResolver = handlerExceptionResolver;
        URI canonical = parseCanonicalOrigin(publicOrigin);
        this.canonicalScheme = canonical.getScheme();
        this.canonicalHost = canonical.getHost();
        this.canonicalPort =
                canonical.getPort() == -1
                        ? defaultPort(canonical.getScheme())
                        : canonical.getPort();
    }

    /** Omite los métodos seguros, que no requieren protección CSRF ni validación de origen. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                && !HttpMethod.DELETE.matches(request.getMethod());
    }

    /** Rechaza un encabezado Origin presente que no represente el origen público configurado. */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && !matchesCanonicalOrigin(origin)) {
            handlerExceptionResolver.resolveException(
                    request, response, null, new CsrfValidationException());
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Compara el Origin sintácticamente válido con esquema, host y puerto públicos configurados.
     */
    private boolean matchesCanonicalOrigin(String origin) {
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
            int actualPort = parsed.getPort() == -1 ? defaultPort(scheme) : parsed.getPort();
            return parsed.getUserInfo() == null
                    && parsed.getPath().isEmpty()
                    && parsed.getQuery() == null
                    && parsed.getFragment() == null
                    && canonicalScheme.equalsIgnoreCase(scheme)
                    && canonicalHost.equalsIgnoreCase(host)
                    && canonicalPort == actualPort;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    /** Valida en el arranque el origen público configurado, sin depender de ninguna petición. */
    private static URI parseCanonicalOrigin(String publicOrigin) {
        if (publicOrigin == null || publicOrigin.isBlank()) {
            throw new IllegalStateException("Falta PMV_WEB_PUBLIC_ORIGIN.");
        }
        try {
            URI parsed = new URI(publicOrigin);
            String scheme = parsed.getScheme();
            if (parsed.isOpaque()
                    || scheme == null
                    || parsed.getHost() == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalStateException(
                        "PMV_WEB_PUBLIC_ORIGIN no es un origen http(s) válido: " + publicOrigin);
            }
            return parsed;
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(
                    "PMV_WEB_PUBLIC_ORIGIN no es un origen válido: " + publicOrigin, exception);
        }
    }

    /** Resuelve el puerto efectivo cuando el Origin utiliza el puerto estándar de su esquema. */
    private static int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }
}
