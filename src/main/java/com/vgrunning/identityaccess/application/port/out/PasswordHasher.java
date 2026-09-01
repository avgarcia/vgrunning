package com.vgrunning.identityaccess.application.port.out;

/** Aísla la aplicación de la implementación concreta del hash de contraseñas. */
public interface PasswordHasher {
    boolean matches(String password, String encodedPassword);

    String hash(String password);

    boolean needsRehash(String encodedPassword);

    String dummyHash();
}
