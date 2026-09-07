package com.vgrunning.runnermanagement.infrastructure.configuration.error;

import com.vgrunning.runnermanagement.application.exception.IdempotencyKeyReusedException;
import com.vgrunning.runnermanagement.application.exception.InvalidRunnerCreationException;
import com.vgrunning.runnermanagement.application.exception.RunnerCreationForbiddenException;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Configuración MVC de los Problem Details propios del alta de corredores. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
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
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, title);
        detail.setType(URI.create("/problems/" + code.replace('_', '-')));
        detail.setTitle(title);
        detail.setProperty("code", code);
        return ResponseEntity.status(status).body(detail);
    }
}
