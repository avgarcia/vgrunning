package com.vgrunning.identityaccess.infrastructure.configuration.error;

import com.vgrunning.identityaccess.application.exception.IdentityAccessException;
import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.exception.RateLimitedException;
import com.vgrunning.identityaccess.infrastructure.security.AuthenticationRequiredException;
import com.vgrunning.identityaccess.infrastructure.security.CsrfValidationException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Configura la representación Problem Details de los fallos de acceso y seguridad. */
@RestControllerAdvice
public class IdentityAccessProblemHandler {

    @ExceptionHandler(AuthenticationRequiredException.class)
    ResponseEntity<ProblemDetail> authenticationRequired(
            AuthenticationRequiredException exception) {
        return problem(HttpStatus.UNAUTHORIZED, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(CsrfValidationException.class)
    ResponseEntity<ProblemDetail> csrfValidationFailed(CsrfValidationException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> invalidCredentials(InvalidCredentialsException exception) {
        return applicationProblem(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ProblemDetail> rateLimited(RateLimitedException exception) {
        long retryAfter = Math.max(1, exception.retryAfter().toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter))
                .body(
                        detail(
                                HttpStatus.TOO_MANY_REQUESTS,
                                exception.code(),
                                exception.getMessage()));
    }

    private static ResponseEntity<ProblemDetail> applicationProblem(
            HttpStatus status, IdentityAccessException exception) {
        return problem(status, exception.code(), exception.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String code, String title) {
        return ResponseEntity.status(status).body(detail(status, code, title));
    }

    private static ProblemDetail detail(HttpStatus status, String code, String title) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, title);
        detail.setType(URI.create("/problems/" + code.replace('_', '-')));
        detail.setTitle(title);
        detail.setProperty("code", code);
        return detail;
    }
}
