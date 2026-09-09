package com.vgrunning.runnermanagement.infrastructure.output.persistence.jooq;

import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository.NewRunner;
import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository.StoredCreation;
import com.vgrunning.runnermanagement.domain.Runner;
import com.vgrunning.runnermanagement.domain.RunnerName;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.vgrunning.generated.jooq.runner_management.tables.records.RunnerRecord;

/** Convierte exclusivamente registros jOOQ del perfil de corredor. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RunnerPersistenceMapper {
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", source = "runnerId")
    @Mapping(target = "status", constant = "pending_activation")
    @Mapping(target = "pendingActivationExpiresAt", source = "activationExpiresAt")
    @Mapping(target = "activatedAt", ignore = true)
    @Mapping(target = "version", constant = "0L")
    RunnerRecord toRunnerRecord(NewRunner runner);

    @Mapping(target = "id", source = "id", qualifiedByName = "required")
    @Mapping(target = "givenName", source = "givenName", qualifiedByName = "requiredName")
    @Mapping(target = "familyName", source = "familyName", qualifiedByName = "requiredName")
    @Mapping(target = "status", source = "status", qualifiedByName = "required")
    Runner toDomain(RunnerRecord runner);

    @Mapping(
            target = "fingerprint",
            expression = "java(java.util.Objects.requireNonNull(fingerprint))")
    @Mapping(
            target = "runner",
            expression =
                    "java(java.util.Objects.requireNonNull(toDomain(java.util.Objects.requireNonNull(runner))))")
    StoredCreation toStoredCreation(byte[] fingerprint, RunnerRecord runner);

    @Named("required")
    static <T> T required(T value) {
        return Objects.requireNonNull(value);
    }

    @Named("requiredName")
    static RunnerName requiredName(String value) {
        return new RunnerName(Objects.requireNonNull(value));
    }
}
