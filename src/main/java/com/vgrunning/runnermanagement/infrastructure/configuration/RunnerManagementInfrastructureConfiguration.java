package com.vgrunning.runnermanagement.infrastructure.configuration;

import com.vgrunning.identityaccess.api.provisioning.AccountProvisioningApi;
import com.vgrunning.runnermanagement.application.mapper.RunnerCreationMapper;
import com.vgrunning.runnermanagement.application.port.in.ActivateRunnerUseCase;
import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase;
import com.vgrunning.runnermanagement.application.port.out.RunnerActivationRepository;
import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository;
import com.vgrunning.runnermanagement.application.service.ActivateRunnerService;
import com.vgrunning.runnermanagement.application.service.CreateRunnerService;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Compone el caso de uso de alta administrativa con sus fronteras publicadas. */
@Configuration(proxyBeanMethods = false)
public class RunnerManagementInfrastructureConfiguration {
    @Bean
    CreateRunnerUseCase createRunnerUseCase(
            AccountProvisioningApi accounts,
            RunnerCreationRepository runners,
            RunnerCreationMapper runnerCreationMapper) {
        return new CreateRunnerService(accounts, runners, runnerCreationMapper);
    }

    @Bean
    ActivateRunnerUseCase activateRunnerUseCase(RunnerActivationRepository runners) {
        return new ActivateRunnerService(runners);
    }

    @Bean
    RunnerCreationMapper runnerCreationMapper() {
        return Mappers.getMapper(RunnerCreationMapper.class);
    }
}
