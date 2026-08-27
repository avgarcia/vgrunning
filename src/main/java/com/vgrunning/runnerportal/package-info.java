/**
 * Delimita la fachada que compone la información del corredor para su portal.
 *
 * <p>No es fuente de verdad ni tendrá esquema propio. Puede consultar contratos públicos de
 * corredores, publicaciones y seguimiento, sin acceder a las implementaciones de esos módulos.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(
        id = "runner-portal",
        displayName = "Runner portal",
        allowedDependencies = {
            "runner-management::api",
            "publication::api",
            "tracking-review::api"
        })
package com.vgrunning.runnerportal;
