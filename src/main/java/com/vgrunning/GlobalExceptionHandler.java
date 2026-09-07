package com.vgrunning;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Manejador global de excepciones para la API. Asegura que todas las respuestas de error sigan el
 * estándar Problem Details (RFC 7807), alineado con las reglas de Spectral para la API HTTP
 * (ADR-0017).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Intercepta excepciones genéricas o de dominio (a implementar en el futuro) y las convierte en
     * respuestas consistentes.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllUncaughtException(Exception ex) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Se ha producido un error interno inesperado.");
        problemDetail.setTitle("Internal Server Error");
        return problemDetail;
    }
}
