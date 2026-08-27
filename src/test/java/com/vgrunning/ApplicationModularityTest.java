package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifica que la aplicación conserva los ocho módulos y el grafo de dependencias aceptado en
 * ADR-0014.
 */
class ApplicationModularityTest {

    private static final ApplicationModules MODULES =
            ApplicationModules.of(RunningCoachApplication.class);

    /**
     * Comprueba que Spring Modulith no detecta ciclos ni accesos a paquetes internos entre módulos
     * reales.
     */
    @Test
    void verifiesTheAcceptedModuleBoundaries() {
        assertThatCode(MODULES::verify).doesNotThrowAnyException();
    }

    /**
     * Comprueba que el análisis descubre exactamente los ocho módulos acordados, sin módulos
     * accidentales.
     */
    @Test
    void recognizesExactlyTheEightAcceptedModules() {
        Set<String> moduleIdentifiers =
                MODULES.stream()
                        .map(module -> module.getIdentifier().toString())
                        .collect(Collectors.toSet());

        assertThat(moduleIdentifiers)
                .containsExactlyInAnyOrder(
                        "identity-access",
                        "runner-management",
                        "classification-segmentation",
                        "planning",
                        "publication",
                        "notification-delivery",
                        "tracking-review",
                        "runner-portal");
    }
}
