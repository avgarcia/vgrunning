/**
 * Delimita la identidad del usuario y sus autorizaciones en Running Coach.
 *
 * <p>Los demás módulos solo podrán consumir contratos publicados desde el subpaquete {@code api}.
 * La autenticación de producto se implementa mediante los componentes de infraestructura de este
 * módulo.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(
        id = "identity-access",
        displayName = "Identity and access",
        allowedDependencies = {"notification-delivery::api"})
package com.vgrunning.identityaccess;
