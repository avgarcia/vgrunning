package com.vgrunning.identityaccess.application.port.out;

import com.vgrunning.identityaccess.application.securityevent.SecurityEvent;

/** Almacena eventos append-only ya decididos por la aplicación. */
public interface SecurityEventRepository {
    void append(SecurityEvent event);
}
