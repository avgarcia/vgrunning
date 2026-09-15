package com.vgrunning.identityaccess.api.actor;

/** Resuelve el actor autenticado de la petición protegida en curso. */
public interface CurrentActor {

    /**
     * Exige una sesión ya autenticada.
     *
     * @throws RuntimeException el que decida la implementación cuando no hay sesión
     */
    ActorContext require();
}
