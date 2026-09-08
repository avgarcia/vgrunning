package com.vgrunning.runnermanagement.infrastructure.configuration;

import com.vgrunning.identityaccess.api.provisioning.AccountProvisioningApi;
import com.vgrunning.runnermanagement.application.port.in.ActivateRunnerUseCase;
import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase;
import com.vgrunning.runnermanagement.application.port.out.RunnerActivationRepository;
import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository;
import com.vgrunning.runnermanagement.application.service.ActivateRunnerService;
import com.vgrunning.runnermanagement.application.service.CreateRunnerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Compone el caso de uso de alta administrativa con sus fronteras publicadas. */
@Configuration(proxyBeanMethods = false)
public class RunnerManagementInfrastructureConfiguration {
    @Bean
    CreateRunnerUseCase createRunnerUseCase(
            AccountProvisioningApi accounts, RunnerCreationRepository runners) {
        return new CreateRunnerService(accounts, runners);
    }

    @Bean
    ActivateRunnerUseCase activateRunnerUseCase(RunnerActivationRepository runners) {
        return new ActivateRunnerService(runners);
    }
}
