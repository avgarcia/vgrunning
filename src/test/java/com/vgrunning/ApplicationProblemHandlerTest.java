package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

/** Prueba unitaria del catch-all de aplicación, fuera de los módulos. */
class ApplicationProblemHandlerTest {
    private final ApplicationProblemHandler handler = new ApplicationProblemHandler();

    @Test
    void representsUnhandledRuntimeExceptionsAsAnOpaqueInternalError() {
        ResponseEntity<ProblemDetail> response =
                handler.internalError(new IllegalStateException("boom"));

        assertProblem(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal_error",
                "Se ha producido un error interno");
    }

    @Test
    void rendersMalformedRequestBodiesAsInvalidRequestWithoutLeakingTheParseFailure() {
        ResponseEntity<Object> response =
                handler.handleHttpMessageNotReadable(
                        new HttpMessageNotReadableException(
                                "Unexpected end-of-input", new MockHttpInputMessage(new byte[0])),
                        new HttpHeaders(),
                        HttpStatus.BAD_REQUEST,
                        webRequest());

        assertProblem(
                response, HttpStatus.BAD_REQUEST, "invalid_request", "La solicitud no es válida");
    }

    @Test
    void rendersFailedBeanValidationAsInvalidRequest() throws NoSuchMethodException {
        Method target =
                ApplicationProblemHandlerTest.class.getDeclaredMethod("target", String.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        new MethodParameter(target, 0),
                        new BeanPropertyBindingResult(new Object(), "command"));

        ResponseEntity<Object> response =
                handler.handleMethodArgumentNotValid(
                        exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest());

        assertProblem(
                response, HttpStatus.BAD_REQUEST, "invalid_request", "La solicitud no es válida");
    }

    @SuppressWarnings("unused")
    private void target(String value) {}

    private static ServletWebRequest webRequest() {
        return new ServletWebRequest(new MockHttpServletRequest());
    }

    private static void assertProblem(
            ResponseEntity<?> response, HttpStatus status, String code, String title) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        ProblemDetail detail = (ProblemDetail) Objects.requireNonNull(response.getBody());
        assertThat(detail.getType()).hasToString("/problems/" + code.replace('_', '-'));
        assertThat(detail.getProperties()).containsEntry("code", code);
        assertThat(detail.getTitle()).isEqualTo(title);
    }
}
