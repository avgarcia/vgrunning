package com.vgrunning.runnermanagement.infrastructure.configuration.error;

import com.vgrunning.ProblemDetails;
import com.vgrunning.runnermanagement.application.exception.IdempotencyKeyReusedException;
import com.vgrunning.runnermanagement.application.exception.InvalidRunnerCreationException;
import com.vgrunning.runnermanagement.application.exception.RunnerCreationForbiddenException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Configuración MVC de los Problem Details propios del alta de corredores.
 *
 * <p>El catch-all de {@code RuntimeException} vive en {@code
 * com.vgrunning.ApplicationProblemHandler}, fuera de los módulos: este advice se ordena por delante
 * ({@code @Order(0)}) para que sus handlers específicos ganen frente a ese catch-all sin depender
 * de compartir clase con él.
 */
@RestControllerAdvice
@Order(0)
public class RunnerManagementProblemHandler {
    @ExceptionHandler(RunnerCreationForbiddenException.class)
    ResponseEntity<ProblemDetail> forbidden(RunnerCreationForbiddenException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    ResponseEntity<ProblemDetail> conflict(IdempotencyKeyReusedException exception) {
        return problem(HttpStatus.CONFLICT, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(InvalidRunnerCreationException.class)
    ResponseEntity<ProblemDetail> invalid(InvalidRunnerCreationException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String code, String title) {
        return ResponseEntity.status(status).body(ProblemDetails.detail(status, code, title));
    }
}
