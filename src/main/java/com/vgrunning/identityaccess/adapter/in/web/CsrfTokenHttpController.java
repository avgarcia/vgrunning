package com.vgrunning.identityaccess.adapter.in.web;

import com.vgrunning.identityaccess.infrastructure.security.SpringCsrfTokenManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.vgrunning.generated.openapi.server.api.CsrfTokensApi;
import org.vgrunning.generated.openapi.server.model.CsrfToken;

/** Expone el token CSRF que Spring Security vincula al navegador actual. */
@RestController
@RequiredArgsConstructor
public class CsrfTokenHttpController implements CsrfTokensApi {
    private final SpringCsrfTokenManager csrfTokens;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Override
    public ResponseEntity<CsrfToken> getCurrentCsrfToken() {
        return ResponseEntity.ok(new CsrfToken(csrfTokens.current(request, response)));
    }
}
