package com.vgrunning.identityaccess.application.port.in;

import com.vgrunning.identityaccess.application.model.SessionLogin;

/** Puerto de entrada para autenticar credenciales y crear una sesión opaca. */
public interface CreateSessionUseCase {
    SessionLogin create(String email, String password, String remoteAddress);
}
