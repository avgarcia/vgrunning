package com.vgrunning.runnermanagement.infrastructure.input.web;

import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase.CreateRunner;
import com.vgrunning.runnermanagement.domain.Runner;
import com.vgrunning.runnermanagement.domain.RunnerName;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.vgrunning.generated.openapi.server.model.RunnerCreation;

/** Traduce el contrato HTTP y el principal técnico a contratos del caso de uso. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RunnerWebMapper {
    CreateRunner toCommand(RunnerCreation request);

    org.vgrunning.generated.openapi.server.model.Runner toResponse(Runner runner);

    default org.vgrunning.generated.openapi.server.model.Runner.StatusEnum map(String status) {
        return org.vgrunning.generated.openapi.server.model.Runner.StatusEnum.fromValue(status);
    }

    default String map(RunnerName name) {
        return name.value();
    }
}
