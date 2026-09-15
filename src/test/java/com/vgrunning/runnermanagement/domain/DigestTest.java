package com.vgrunning.runnermanagement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Comprueba el vector conocido de SHA-256 tras mover el cálculo del puerto al dominio. */
class DigestTest {

    @Test
    void computesTheKnownSha256VectorForAbc() {
        byte[] digest = Digest.sha256("abc");

        assertThat(HexFormat.of().formatHex(digest))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void isDeterministicForTheSameInput() {
        assertThat(Digest.sha256("running-coach")).isEqualTo(Digest.sha256("running-coach"));
    }
}
