/**
 * Delimita la clasificación y segmentación de corredores que utilizará la planificación.
 *
 * <p>Puede consultar los contratos públicos de {@code runner-management}; sus futuros consumidores
 * deberán usar exclusivamente contratos que publique en {@code api}.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(
        id = "classification-segmentation",
        displayName = "Classification and segmentation",
        allowedDependencies = {"runner-management::api"})
package com.vgrunning.classificationsegmentation;
