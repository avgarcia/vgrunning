package com.vgrunning.identityaccess.application.port.out;

/** Genera secretos aleatorios de un solo uso para desafíos de acceso. */
public interface SecretGenerator {
    String generateUrlSafeSecret();
}
