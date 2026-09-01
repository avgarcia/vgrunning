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

/** Rechaza orígenes declarados que no correspondan al host directo de la solicitud. */
public final class OriginValidationFilter extends OncePerRequestFilter {
    private final HandlerExceptionResolver handlerExceptionResolver;

    public OriginValidationFilter(HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                && !HttpMethod.DELETE.matches(request.getMethod());
    }

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

    private static boolean matchesRequestOrigin(String origin, HttpServletRequest request) {
        try {
            URI parsed = new URI(origin);
            int expectedPort = request.getServerPort();
            int actualPort =
                    parsed.getPort() == -1 ? defaultPort(parsed.getScheme()) : parsed.getPort();
            return parsed.getUserInfo() == null
                    && parsed.getPath().isEmpty()
                    && parsed.getQuery() == null
                    && parsed.getFragment() == null
                    && request.getScheme().equalsIgnoreCase(parsed.getScheme())
                    && request.getServerName().equalsIgnoreCase(parsed.getHost())
                    && expectedPort == actualPort;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }
}
