package com.vgrunning.identityaccess.application.port.in;

/** Puerto local para aprovisionar las cuentas sintéticas del entorno permitido. */
public interface ProvisionSyntheticAccountsUseCase {
    void provision(Command command);

    record Command(String administratorPassword, String runnerPassword) {}
}
