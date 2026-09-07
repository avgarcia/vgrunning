package com.vgrunning.identityaccess.api.provisioning;

/** Crea la identidad mínima de un corredor dentro de la transacción del alta. */
public interface AccountProvisioningApi {
    ProvisionedRunnerAccount provision(ProvisionRunnerAccount command);
}
