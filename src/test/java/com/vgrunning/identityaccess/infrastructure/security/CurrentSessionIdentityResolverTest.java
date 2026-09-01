package com.vgrunning.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.application.model.SessionIdentity;
import com.vgrunning.identityaccess.domain.account.AccountRole;
import com.vgrunning.identityaccess.domain.account.AccountStatus;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentSessionIdentityResolverTest {
    private final CurrentSessionIdentityResolver resolver = new CurrentSessionIdentityResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheAuthenticatedSessionIdentity() {
        SessionIdentity identity = identity();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                identity, null, java.util.List.of()));

        assertThat(resolver.current()).isSameAs(identity);
    }

    @Test
    void rejectsMissingOrUnsupportedAuthentication() {
        assertThatThrownBy(resolver::current).isInstanceOf(AuthenticationRequiredException.class);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "not-an-identity", null, java.util.List.of()));

        assertThatThrownBy(resolver::current).isInstanceOf(AuthenticationRequiredException.class);
    }

    private static SessionIdentity identity() {
        return SessionIdentity.restore(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                AccountRole.CORREDOR,
                AccountStatus.ACTIVE);
    }
}
