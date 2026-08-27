/**
 * Delimita la publicación de planes para que puedan ser consultados por sus destinatarios.
 *
 * <p>Puede coordinar contratos públicos de planificación, corredores y entrega de notificaciones.
 * Sus consumidores deberán depender solo de su subpaquete {@code api}.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(
        id = "publication",
        displayName = "Publication",
        allowedDependencies = {
            "planning::api",
            "runner-management::api",
            "notification-delivery::api"
        })
package com.vgrunning.publication;
