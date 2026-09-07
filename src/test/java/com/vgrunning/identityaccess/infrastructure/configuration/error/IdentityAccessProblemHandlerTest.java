package com.vgrunning.identityaccess.infrastructure.configuration.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.infrastructure.security.AuthenticationRequiredException;
import com.vgrunning.identityaccess.infrastructure.security.CsrfValidationException;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.RateLimitedException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class IdentityAccessProblemHandlerTest {
    private final IdentityAccessProblemHandler handler = new IdentityAccessProblemHandler();

    @Test
    void representsAuthenticationAndCsrfFailures() {
        assertProblem(
                handler.authenticationRequired(new AuthenticationRequiredException()),
                HttpStatus.UNAUTHORIZED,
                "authentication_required");
        assertProblem(
                handler.csrfValidationFailed(new CsrfValidationException()),
                HttpStatus.FORBIDDEN,
                "csrf_validation_failed");
    }

    @Test
    void representsApplicationFailuresFromTheirOwnSemanticData() {
        assertProblem(
                handler.invalidCredentials(new InvalidCredentialsException()),
                HttpStatus.UNAUTHORIZED,
                "session_creation_rejected");

        ResponseEntity<ProblemDetail> limited =
                handler.rateLimited(new RateLimitedException(Duration.ofSeconds(19)));
        assertProblem(limited, HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded");
        assertThat(limited.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("19");
    }

    @Test
    void neverEmitsAZeroRetryAfterHeader() {
        ResponseEntity<ProblemDetail> limited =
                handler.rateLimited(new RateLimitedException(Duration.ZERO));

        assertThat(limited.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
    }

    private static void assertProblem(
            ResponseEntity<ProblemDetail> response, HttpStatus status, String code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail detail = response.getBody();
        assertThat(detail.getStatus()).isEqualTo(status.value());
        assertThat(detail.getType()).hasToString("/problems/" + code.replace('_', '-'));
        assertThat(detail.getProperties()).containsEntry("code", code);
        assertThat(detail.getTitle()).isEqualTo(detail.getDetail());
    }
}
