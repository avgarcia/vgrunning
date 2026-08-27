/**
 * Delimita el seguimiento de la actividad del corredor y su revisión por el entrenador.
 *
 * <p>Puede consultar contratos publicados de planes y corredores. Cualquier módulo consumidor
 * deberá utilizar exclusivamente sus contratos bajo {@code api}.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(
        id = "tracking-review",
        displayName = "Tracking and review",
        allowedDependencies = {"publication::api", "runner-management::api"})
package com.vgrunning.trackingreview;
