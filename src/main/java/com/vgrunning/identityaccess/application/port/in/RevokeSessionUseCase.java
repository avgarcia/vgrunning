package com.vgrunning.identityaccess.application.port.in;

import com.vgrunning.identityaccess.application.model.SessionIdentity;

/** Puerto de entrada para revocar la sesión actual. */
public interface RevokeSessionUseCase {
    void revoke(SessionIdentity session);
}
