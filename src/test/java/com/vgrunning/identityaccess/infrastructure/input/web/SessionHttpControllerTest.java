package com.vgrunning.identityaccess.infrastructure.input.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.application.exception.InvalidCredentialsException;
import com.vgrunning.identityaccess.application.port.in.AuthenticateCredentialsUseCase;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.LoginRateLimiter;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.RateLimitedException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.vgrunning.generated.openapi.server.model.SessionCreation;

/** Comprueba que el límite técnico usa la misma canonicalización que la autenticación. */
class SessionHttpControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changesTheExistingSessionIdWhenCredentialsAreAccepted() {
        MockHttpServletRequest request = request();
        request.setSession(new MockHttpSession());
        String sessionIdBeforeAuthentication = request.getSession().getId();
        SessionHttpController controller =
                new SessionHttpController(
                        successfulAuthentication(),
                        new LoginRateLimiter(new SimpleMeterRegistry()),
                        new HttpSessionSecurityContextRepository(),
                        new ChangeSessionIdAuthenticationStrategy(),
                        null,
                        null,
                        Mappers.getMapper(SessionWebMapper.class),
                        request,
                        new MockHttpServletResponse());

        controller.createSession(new SessionCreation("runner@example.invalid", "password"));

        assertThat(request.getSession().getId()).isNotEqualTo(sessionIdBeforeAuthentication);
    }

    @Test
    void sharesOneRateLimitBucketAcrossEquivalentEmailVariants() {
        SessionHttpController controller =
                new SessionHttpController(
                        failingAuthentication(),
                        new LoginRateLimiter(new SimpleMeterRegistry()),
                        null,
                        null,
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
            assertThatThrownBy(
                            () -> controller.createSession(new SessionCreation(email, "password")))
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

    private static AuthenticateCredentialsUseCase successfulAuthentication() {
        return (email, password) ->
                new AuthenticateCredentialsUseCase.AuthenticatedAccount(
                        UUID.fromString("10000000-0000-0000-0000-000000000001"),
                        AccountRole.CORREDOR,
                        AccountStatus.ACTIVE);
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
