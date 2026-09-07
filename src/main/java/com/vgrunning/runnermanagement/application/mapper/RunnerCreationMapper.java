package com.vgrunning.runnermanagement.application.mapper;

import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase.CreatedRunner;
import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository.StoredRunner;
import org.mapstruct.Mapper;

/** Convierte el resultado de persistencia al contrato del caso de uso local. */
@Mapper
public interface RunnerCreationMapper {
    CreatedRunner toCreatedRunner(StoredRunner runner);
}
