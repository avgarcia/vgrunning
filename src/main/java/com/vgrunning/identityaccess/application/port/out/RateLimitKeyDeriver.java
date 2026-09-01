package com.vgrunning.identityaccess.application.port.out;

/** Deriva identificadores no reversibles para los contadores de autenticación. */
public interface RateLimitKeyDeriver {
    byte[] accountKey(String canonicalEmail);

    byte[] ipKey(String remoteAddress);
}
