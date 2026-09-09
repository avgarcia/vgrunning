package com.vgrunning.identityaccess.api.actor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.UUID;
import java.util.function.Supplier;

/** Actor autenticado que los adaptadores entregan explícitamente a los casos de uso protegidos. */
public record ActorContext(UUID accountId, String role) {
    public boolean isAdministrator() {
        return "administrador".equals(role);
    }

    /** Exige rol de administrador; lanza la excepción del llamante en caso contrario. */
    @SuppressFBWarnings(
            value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION",
            justification =
                    "El tipo concreto lo decide cada módulo llamante (InvitationProvisioning"
                            + "ForbiddenException, RunnerCreationForbiddenException); el contrato"
                            + " deliberadamente no lo fija aquí.")
    public void requireAdministrator(Supplier<? extends RuntimeException> exceptionIfForbidden) {
        if (!isAdministrator()) {
            throw exceptionIfForbidden.get();
        }
    }
}
