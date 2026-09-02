package com.vgrunning.identityaccess.infrastructure.input.command;

import com.vgrunning.identityaccess.application.port.in.ProvisionSyntheticAccountsUseCase;
import com.vgrunning.identityaccess.infrastructure.configuration.synthetic.SyntheticAccountProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/** Entrada operativa que inicia el aprovisionamiento sintético una vez al arrancar. */
@RequiredArgsConstructor
public final class SyntheticAccountBootstrap implements ApplicationRunner {
    private final ProvisionSyntheticAccountsUseCase provisionAccounts;
    private final SyntheticAccountProperties properties;

    @Override
    public void run(ApplicationArguments arguments) {
        provisionAccounts.provision(
                new ProvisionSyntheticAccountsUseCase.Command(
                        properties.administratorPassword(), properties.runnerPassword()));
    }
}
