package com.vgrunning.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountRole;
import com.vgrunning.identityaccess.domain.account.valueobject.AccountStatus;
import com.vgrunning.identityaccess.infrastructure.security.session.SessionPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextCurrentActorTest {
    private final SecurityContextCurrentActor currentActor =
            new SecurityContextCurrentActor(new CurrentSessionIdentityResolver());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesTheAuthenticatedActorFromTheSecurityContext() {
        UUID accountId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        SessionPrincipal identity =
                new SessionPrincipal(accountId, AccountRole.ADMINISTRADOR, AccountStatus.ACTIVE);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                identity, null, List.of()));

        ActorContext actor = currentActor.require();

        assertThat(actor.accountId()).isEqualTo(accountId);
        assertThat(actor.role()).isEqualTo("administrador");
        assertThat(actor.isAdministrator()).isTrue();
    }

    @Test
    void rejectsMissingAuthenticationInsteadOfReturningNull() {
        assertThatThrownBy(currentActor::require)
                .isInstanceOf(AuthenticationRequiredException.class);
    }
}
