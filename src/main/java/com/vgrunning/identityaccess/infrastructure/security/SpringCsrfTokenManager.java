package com.vgrunning.identityaccess.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/** Encapsula el ciclo de vida técnico de los tokens CSRF gestionados por Spring Security. */
public final class SpringCsrfTokenManager {
    private final CsrfTokenRepository repository;

    public SpringCsrfTokenManager(CsrfTokenRepository repository) {
        this.repository = repository;
    }

    public String current(HttpServletRequest request, HttpServletResponse response) {
        return Optional.ofNullable(repository.loadToken(request))
                .orElseGet(() -> generateAndSave(request, response))
                .getToken();
    }

    public void rotate(HttpServletRequest request, HttpServletResponse response) {
        repository.saveToken(null, request, response);
        generateAndSave(request, response);
    }

    private CsrfToken generateAndSave(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = repository.generateToken(request);
        repository.saveToken(token, request, response);
        return token;
    }
}
