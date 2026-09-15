package com.vgrunning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Catch-all de la aplicación completa, no de un módulo: vive fuera de los paquetes de módulo para
 * que su precedencia sea explícita frente a los advices de cada módulo, en lugar de depender de que
 * compartan clase con sus handlers específicos.
 *
 * <p>{@code ExceptionHandlerExceptionResolver} elige el handler más específico dentro de cada
 * advice, no entre advices: por eso este catch-all se ordena explícitamente al final ({@link
 * Ordered#LOWEST_PRECEDENCE}) y cada advice de módulo se ordena por delante.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApplicationProblemHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProblemHandler.class);

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
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ProblemDetails.detail(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "internal_error",
                                "Se ha producido un error interno"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return ResponseEntity.status(status)
                .headers(headers)
                .body(
                        (Object)
                                ProblemDetails.detail(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_request",
                                        "La solicitud no es válida"));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return ResponseEntity.status(status)
                .headers(headers)
                .body(
                        (Object)
                                ProblemDetails.detail(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_request",
                                        "La solicitud no es válida"));
    }
}
