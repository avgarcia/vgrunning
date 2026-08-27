/**
 * Delimita la decisión y el registro de la entrega de notificaciones.
 *
 * <p>No depende de APIs de negocio de otros módulos. Los módulos que soliciten una notificación
 * usarán sus contratos públicos en {@code api}, sin acoplarse a detalles de entrega.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(
        id = "notification-delivery",
        displayName = "Notification delivery")
package com.vgrunning.notificationdelivery;
