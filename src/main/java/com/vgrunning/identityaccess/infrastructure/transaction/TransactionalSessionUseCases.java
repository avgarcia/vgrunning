package com.vgrunning.identityaccess.infrastructure.transaction;

import com.vgrunning.identityaccess.application.model.SessionIdentity;
import com.vgrunning.identityaccess.application.model.SessionLogin;
import com.vgrunning.identityaccess.application.port.in.CreateSessionUseCase;
import com.vgrunning.identityaccess.application.port.in.ResolveSessionUseCase;
import com.vgrunning.identityaccess.application.port.in.RevokeSessionUseCase;
import com.vgrunning.identityaccess.application.usecase.SessionUseCaseHandler;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionTemplate;

/** Aplica la transacción Spring alrededor de los casos de uso sin contaminar aplicación. */
@RequiredArgsConstructor
public final class TransactionalSessionUseCases
        implements CreateSessionUseCase, ResolveSessionUseCase, RevokeSessionUseCase {
    private final SessionUseCaseHandler delegate;
    private final TransactionTemplate transactions;

    @Override
    public SessionLogin create(String email, String password, String remoteAddress) {
        return Objects.requireNonNull(
                transactions.execute(status -> delegate.create(email, password, remoteAddress)));
    }

    @Override
    public Optional<SessionIdentity> resolve(String rawSessionToken) {
        return Objects.requireNonNull(
                transactions.execute(status -> delegate.resolve(rawSessionToken)));
    }

    @Override
    public void revoke(SessionIdentity session) {
        transactions.executeWithoutResult(status -> delegate.revoke(session));
    }
}
