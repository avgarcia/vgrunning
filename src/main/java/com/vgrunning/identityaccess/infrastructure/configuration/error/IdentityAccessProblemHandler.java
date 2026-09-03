package com.vgrunning.identityaccess.infrastructure.configuration.error;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.infrastructure.security.AuthenticationRequiredException;
import com.vgrunning.identityaccess.infrastructure.security.CsrfValidationException;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.RateLimitedException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Configura la representación Problem Details de los fallos de acceso y seguridad. */
@RestControllerAdvice
public class IdentityAccessProblemHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IdentityAccessProblemHandler.class);

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
        return problem(HttpStatus.UNAUTHORIZED, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ProblemDetail> rateLimited(RateLimitedException exception) {
        long retryAfter =
                Math.max(1, Math.ceilDiv(exception.retryAfter().toNanos(), 1_000_000_000L));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter))
                .body(
                        detail(
                                HttpStatus.TOO_MANY_REQUESTS,
                                exception.code(),
                                exception.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ProblemDetail> internalError(RuntimeException exception) {
        LOGGER.atError()
                .addKeyValue("exceptionType", exception.getClass().getName())
                .addKeyValue(
                        "rootCauseType",
                        exception.getCause() == null
                                ? exception.getClass().getName()
                                : exception.getCause().getClass().getName())
                .log("Fallo no controlado al procesar una solicitud");
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal_error",
                "Se ha producido un error interno");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            org.springframework.http.HttpStatusCode status,
            WebRequest request) {
        return ResponseEntity.status(status)
                .headers(headers)
                .body(
                        (Object)
                                detail(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_request",
                                        "La solicitud no es válida"));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            org.springframework.http.HttpStatusCode status,
            WebRequest request) {
        return ResponseEntity.status(status)
                .headers(headers)
                .body(
                        (Object)
                                detail(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_request",
                                        "La solicitud no es válida"));
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
