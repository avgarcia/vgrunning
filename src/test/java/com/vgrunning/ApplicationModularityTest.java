package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(RunningCoachApplication.class);

    @Test
    void verifiesTheAcceptedModuleBoundaries() {
        assertThatCode(MODULES::verify).doesNotThrowAnyException();
    }

    @Test
    void recognizesExactlyTheEightAcceptedModules() {
        Set<String> moduleIdentifiers = MODULES.stream()
            .map(module -> module.getIdentifier().toString())
            .collect(Collectors.toSet());

        assertThat(moduleIdentifiers).containsExactlyInAnyOrder(
            "identity-access",
            "runner-management",
            "classification-segmentation",
            "planning",
            "publication",
            "notification-delivery",
            "tracking-review",
            "runner-portal"
        );
    }
}
