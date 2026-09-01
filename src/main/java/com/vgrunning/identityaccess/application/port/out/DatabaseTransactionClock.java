package com.vgrunning.identityaccess.application.port.out;

import java.time.OffsetDateTime;

/** Devuelve el instante del PostgreSQL que participa en la transacción actual. */
public interface DatabaseTransactionClock {
    OffsetDateTime now();
}
