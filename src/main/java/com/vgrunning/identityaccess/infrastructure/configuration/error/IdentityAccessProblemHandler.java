package com.vgrunning.identityaccess.infrastructure.configuration.error;

import com.vgrunning.ProblemDetails;
import com.vgrunning.identityaccess.application.exception.EmailAlreadyReservedException;
import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.exception.InvalidInvitationAcceptanceException;
import com.vgrunning.identityaccess.application.exception.InvitationNotAvailableException;
import com.vgrunning.identityaccess.application.exception.InvitationProvisioningForbiddenException;
import com.vgrunning.identityaccess.infrastructure.security.AuthenticationRequiredException;
import com.vgrunning.identityaccess.infrastructure.security.CsrfValidationException;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.RateLimitedException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Configura la representación Problem Details de los fallos propios de acceso y seguridad.
 *
 * <p>El catch-all de {@code RuntimeException} vive en {@code
 * com.vgrunning.ApplicationProblemHandler}, fuera de los módulos: este advice se ordena por delante
 * ({@code @Order(0)}) para que sus handlers específicos ganen frente a ese catch-all sin depender
 * de compartir clase con él.
 */
@RestControllerAdvice
@Order(0)
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
        return problem(HttpStatus.UNAUTHORIZED, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(InvalidInvitationAcceptanceException.class)
    ResponseEntity<ProblemDetail> invalidInvitationAcceptance(
            InvalidInvitationAcceptanceException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(InvitationNotAvailableException.class)
    ResponseEntity<ProblemDetail> invitationNotAvailable(
            InvitationNotAvailableException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(EmailAlreadyReservedException.class)
    ResponseEntity<ProblemDetail> emailAlreadyReserved(EmailAlreadyReservedException exception) {
        return problem(HttpStatus.CONFLICT, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(InvitationProvisioningForbiddenException.class)
    ResponseEntity<ProblemDetail> invitationProvisioningForbidden(
            InvitationProvisioningForbiddenException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ProblemDetail> rateLimited(RateLimitedException exception) {
        long retryAfter =
                Math.max(1, Math.ceilDiv(exception.retryAfter().toNanos(), 1_000_000_000L));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter))
                .body(
                        ProblemDetails.detail(
                                HttpStatus.TOO_MANY_REQUESTS,
                                exception.code(),
                                exception.getMessage()));
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String code, String title) {
        return ResponseEntity.status(status).body(ProblemDetails.detail(status, code, title));
    }
}
