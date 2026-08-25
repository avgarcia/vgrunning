/**
 * Delimita la identidad del usuario y sus autorizaciones en Running Coach.
 *
 * <p>Los demás módulos solo podrán consumir contratos publicados desde el subpaquete {@code api}.
 * La autenticación de producto no forma parte todavía de este esqueleto.</p>
 */
@org.springframework.modulith.ApplicationModule(
    id = "identity-access",
    displayName = "Identity and access",
    allowedDependencies = {"notification-delivery::api"}
)
package com.vgrunning.identityaccess;
