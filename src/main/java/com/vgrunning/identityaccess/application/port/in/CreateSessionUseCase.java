package com.vgrunning.identityaccess.application.port.in;

import com.vgrunning.identityaccess.application.model.SessionLogin;

/** Puerto de entrada para autenticar credenciales. */
public interface CreateSessionUseCase {
    SessionLogin create(String email, String password);
}
