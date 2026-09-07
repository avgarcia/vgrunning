package com.vgrunning.runnermanagement.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository.StoredRunner;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RunnerCreationMapperTest {
    private final RunnerCreationMapper mapper = Mappers.getMapper(RunnerCreationMapper.class);

    @Test
    void mapsTheStoredRunnerWithoutChangingItsPublicRepresentation() {
        StoredRunner stored =
                new StoredRunner(UUID.randomUUID(), "Lucía", "Martín", "pending_activation");

        assertThat(mapper.toCreatedRunner(stored))
                .extracting("id", "givenName", "familyName", "status")
                .containsExactly(
                        stored.id(), stored.givenName(), stored.familyName(), stored.status());
        assertThat(mapper.toCreatedRunner(null)).isNull();
    }
}
