package com.vgrunning.qualityfixture;

import java.util.ArrayList;
import java.util.List;

/** Fixture permanente: javac debe rechazar el uso de tipos raw mediante -Werror. */
final class RawTypeWarning {

    private RawTypeWarning() {}

    static List invalidRawType() {
        return new ArrayList<String>();
    }
}
