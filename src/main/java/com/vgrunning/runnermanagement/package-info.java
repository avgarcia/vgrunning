/**
 * Delimita la gestión de los corredores, incluidos sus datos y su ciclo de vida.
 *
 * <p>Los módulos que necesiten información de un corredor usarán únicamente contratos del
 * subpaquete {@code api}; las implementaciones internas no son una interfaz compartida.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(
        id = "runner-management",
        displayName = "Runner management",
        allowedDependencies = {"identity-access::api"})
package com.vgrunning.runnermanagement;
