package com.vgrunning.identityaccess.application.port.out;

import java.util.Optional;

/** Aísla la aplicación de la implementación concreta del hash de contraseñas. */
public interface PasswordHasher {
    /**
     * Compara una contraseña sin distinguir públicamente entre hash ausente y hash existente.
     *
     * @param password contraseña normalizada
     * @param encodedPassword hash persistido, cuando la cuenta dispone de uno
     * @return si la contraseña coincide con el hash persistido
     */
    boolean matchesForAuthentication(String password, Optional<String> encodedPassword);

    /** Calcula un hash de contraseña con la política vigente. */
    String hash(String password);

    /** Indica si un hash persistido debe actualizarse a la política vigente. */
    boolean needsRehash(String encodedPassword);
}
