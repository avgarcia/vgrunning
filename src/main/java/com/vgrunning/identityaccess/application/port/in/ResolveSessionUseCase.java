package com.vgrunning.identityaccess.application.port.in;

import com.vgrunning.identityaccess.application.model.SessionIdentity;
import java.util.Optional;

/** Puerto de entrada interno para resolver una cookie de sesión opaca. */
public interface ResolveSessionUseCase {
    Optional<SessionIdentity> resolve(String rawSessionToken);
}
