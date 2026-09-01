package com.vgrunning.identityaccess.application;

/** Resultado efímero de iniciar sesión; el secreto solo se conserva para emitir la cookie. */
public record SessionLogin(SessionIdentity session, String rawSessionToken) {}
