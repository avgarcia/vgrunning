package com.vgrunning.qualityfixture;

/** Fixture permanente: SpotBugs debe detectar una desreferencia nula posible. */
final class SpotBugsViolation {

    private SpotBugsViolation() {}

    @SuppressWarnings("NullAway")
    static int invalidNullDereference(String value) {
        if (value == null) {
            return value.length();
        }
        return value.length();
    }
}
