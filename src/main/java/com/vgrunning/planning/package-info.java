/**
 * Delimita la creación y evolución de planes de entrenamiento.
 *
 * <p>Puede usar los contratos públicos de clasificación y de corredores. La publicación de un plan
 * se realizará a través del módulo {@code publication}, nunca accediendo a sus internos.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(
        id = "planning",
        displayName = "Planning",
        allowedDependencies = {"classification-segmentation::api", "runner-management::api"})
package com.vgrunning.planning;
