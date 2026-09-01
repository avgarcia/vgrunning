package com.vgrunning.identityaccess.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/** Rota la cookie CSRF tras los flujos de autenticación propios de la API. */
@RequiredArgsConstructor
public final class CsrfTokenRotator {
    private final CsrfTokenRepository repository;

    public void rotate(HttpServletRequest request, HttpServletResponse response) {
        repository.saveToken(null, request, response);
        CsrfToken replacement = repository.generateToken(request);
        repository.saveToken(replacement, request, response);
    }
}
