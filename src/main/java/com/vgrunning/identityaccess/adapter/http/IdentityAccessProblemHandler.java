package com.vgrunning.identityaccess.adapter.http;

import com.vgrunning.identityaccess.application.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.RateLimitedException;
import com.vgrunning.identityaccess.adapter.security.AuthenticationRequiredException;
import com.vgrunning.identityaccess.adapter.security.CsrfValidationException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce fallos de aplicación y seguridad a los Problem Details declarados en OpenAPI. */
@RestControllerAdvice(assignableTypes = SessionHttpController.class)
public class IdentityAccessProblemHandler {

    @ExceptionHandler(AuthenticationRequiredException.class)
    ResponseEntity<ProblemDetail> authenticationRequired() {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "/problems/authentication-required",
                "Es necesario iniciar sesión",
                "authentication_required");
    }

    @ExceptionHandler(CsrfValidationException.class)
    ResponseEntity<ProblemDetail> csrfValidationFailed() {
        return problem(
                HttpStatus.FORBIDDEN,
                "/problems/csrf-validation-failed",
                "La solicitud no ha superado la validación de seguridad",
                "csrf_validation_failed");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> invalidCredentials() {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "/problems/session-creation-rejected",
                "No se ha podido iniciar sesión",
                "session_creation_rejected");
    }

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ProblemDetail> rateLimited(RateLimitedException exception) {
        long retryAfter = Math.max(1, exception.retryAfter().toSeconds());
        ProblemDetail detail =
                detail(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "/problems/rate-limit-exceeded",
                        "No se puede intentar el acceso en este momento",
                        "rate_limit_exceeded");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter))
                .body(detail);
    }

    static ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String type, String title, String code) {
        return ResponseEntity.status(status).body(detail(status, type, title, code));
    }

    private static ProblemDetail detail(HttpStatus status, String type, String title, String code) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, title);
        detail.setType(URI.create(type));
        detail.setTitle(title);
        detail.setProperty("code", code);
        return ResponseEntity.status(status).body(detail);
    }
}
