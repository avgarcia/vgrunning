package com.vgrunning.identityaccess.infrastructure.input.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.port.in.AuthenticateCredentialsUseCase;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.LoginRateLimiter;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.RateLimitedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vgrunning.generated.openapi.server.model.SessionCreation;

/** Comprueba que el límite técnico usa la misma canonicalización que la autenticación. */
class SessionHttpControllerTest {

    @Test
    void sharesOneRateLimitBucketAcrossEquivalentEmailVariants() {
        SessionHttpController controller =
                new SessionHttpController(
                        failingAuthentication(),
                        new LoginRateLimiter(),
                        null,
                        null,
                        null,
                        request(),
                        new MockHttpServletResponse());

        for (String email :
                new String[] {
                    "RUNNER@example.invalid",
                    "runner@EXAMPLE.invalid",
                    " runner@example.invalid ",
                    "runner@example.invalid",
                    "RuNnEr@example.invalid"
                }) {
            assertThatThrownBy(() -> controller.createSession(new SessionCreation(email, "password")))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(
                        () ->
                                controller.createSession(
                                        new SessionCreation("runner@example.invalid", "password")))
                .isInstanceOf(RateLimitedException.class);
    }

    private static AuthenticateCredentialsUseCase failingAuthentication() {
        return (email, password) -> {
            throw new InvalidCredentialsException();
        };
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
