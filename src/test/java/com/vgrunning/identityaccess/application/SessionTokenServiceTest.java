package com.vgrunning.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.identityaccess.application.port.out.SessionTokenService;
import org.junit.jupiter.api.Test;

class SessionTokenServiceTest {

    @Test
    void generatedTokenProtectsItsVerifierFromMutation() {
        byte[] source = {1, 2, 3};
        SessionTokenService.GeneratedSessionToken token =
                new SessionTokenService.GeneratedSessionToken("opaque", source);

        source[0] = 9;
        byte[] returned = token.verifier();
        returned[1] = 9;

        assertThat(token.rawToken()).isEqualTo("opaque");
        assertThat(token.verifier()).containsExactly(1, 2, 3);
    }
}
