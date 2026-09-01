package com.vgrunning.identityaccess.application.port.out;

import java.time.OffsetDateTime;

/** Proporciona el instante autoritativo para una transacción crítica. */
public interface CurrentTimeProvider {
    OffsetDateTime now();
}
