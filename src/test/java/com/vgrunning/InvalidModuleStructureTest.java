package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

/**
 * Demuestra que la verificación modular falla ante una infracción deliberada de los límites públicos.
 */
class InvalidModuleStructureTest {

    /** Rechaza que un módulo consumidor importe un tipo situado en el paquete interno de otro módulo. */
    @Test
    void rejectsAnUndeclaredDependencyOnAnotherModulesInternalType() {
        assertThatThrownBy(
            () -> ApplicationModules.of("com.vgrunning.modulithfixture.invalid", location -> true).verify()
        )
            .isInstanceOf(Violations.class);
    }
}
