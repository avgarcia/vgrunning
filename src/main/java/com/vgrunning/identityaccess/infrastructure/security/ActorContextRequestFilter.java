package com.vgrunning.identityaccess.infrastructure.security;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

/** Expone el actor autenticado como atributo de la petición para las entradas protegidas. */
@RequiredArgsConstructor
public final class ActorContextRequestFilter extends OncePerRequestFilter {
    private final CurrentSessionIdentityResolver sessions;
    private final SessionActorMapper mapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        sessions.findCurrentSession()
                .map(mapper::toActorContext)
                .ifPresent(actor -> request.setAttribute(ActorContext.class.getName(), actor));
        filterChain.doFilter(request, response);
    }
}
