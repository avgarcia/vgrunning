package com.vgrunning.modulithfixture.invalid.consumer;

import com.vgrunning.modulithfixture.invalid.source.internal.InternalSourceType;

/**
 * Fixture que viola intencionadamente el límite modular al importar un tipo interno de {@code
 * source}.
 *
 * <p>La prueba {@code InvalidModuleStructureTest} exige que Spring Modulith rechace esta
 * dependencia.
 */
final class ForbiddenInternalDependency {

    private final InternalSourceType source = new InternalSourceType();

    InternalSourceType source() {
        return source;
    }
}
