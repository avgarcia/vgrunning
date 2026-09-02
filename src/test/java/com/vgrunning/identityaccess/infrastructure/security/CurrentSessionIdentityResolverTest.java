package com.vgrunning.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import com.vgrunning.identityaccess.infrastructure.security.session.SessionPrincipal;
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
        SessionPrincipal identity = identity();
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

    private static SessionPrincipal identity() {
        return SessionPrincipal.create(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                AccountRole.CORREDOR,
                AccountStatus.ACTIVE);
    }
}
