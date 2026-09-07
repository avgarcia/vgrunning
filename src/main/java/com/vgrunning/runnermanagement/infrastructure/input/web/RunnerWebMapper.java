package com.vgrunning.runnermanagement.infrastructure.input.web;

import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase.CreateRunner;
import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase.CreatedRunner;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.vgrunning.generated.openapi.server.model.Runner;
import org.vgrunning.generated.openapi.server.model.RunnerCreation;

/** Traduce el contrato HTTP y el principal técnico a contratos del caso de uso. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RunnerWebMapper {
    CreateRunner toCommand(RunnerCreation request);

    Runner toResponse(CreatedRunner runner);

    default Runner.StatusEnum map(String status) {
        return Runner.StatusEnum.fromValue(status);
    }
}
