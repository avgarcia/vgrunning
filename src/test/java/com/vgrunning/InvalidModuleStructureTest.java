package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

class InvalidModuleStructureTest {

    @Test
    void rejectsAnUndeclaredDependencyOnAnotherModulesInternalType() {
        assertThatThrownBy(
            () -> ApplicationModules.of("com.vgrunning.modulithfixture.invalid", location -> true).verify()
        )
            .isInstanceOf(Violations.class);
    }
}
