package com.vgrunning.qualityfixture;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Fixture permanente: NullAway debe impedir desreferenciar un valor anulable. */
@NullMarked
final class NullAwayViolation {

    private NullAwayViolation() {}

    static int invalidUnboxing(@Nullable Integer value) {
        return value;
    }
}
