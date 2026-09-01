package com.vgrunning.identityaccess.application.port.out;

import java.time.OffsetDateTime;

/** Persistencia atómica de contadores de fallos de acceso; no decide la política. */
public interface LoginRateLimitRepository {
    FailureCounts currentFailures(byte[] accountKey, byte[] ipKey, OffsetDateTime windowStart);

    FailureCounts incrementFailures(
            byte[] accountKey,
            byte[] ipKey,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            OffsetDateTime now);

    record FailureCounts(int account, int ip) {}
}
