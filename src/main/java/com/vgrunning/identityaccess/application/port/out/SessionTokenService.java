package com.vgrunning.identityaccess.application.port.out;

/** Genera tokens opacos y deriva exclusivamente el verificador que se persiste. */
public interface SessionTokenService {
    GeneratedSessionToken generate();

    byte[] verifier(String rawToken);

    final class GeneratedSessionToken {
        private final String rawToken;
        private final byte[] verifier;

        public GeneratedSessionToken(String rawToken, byte[] verifier) {
            this.rawToken = rawToken;
            this.verifier = verifier.clone();
        }

        public String rawToken() {
            return rawToken;
        }

        public byte[] verifier() {
            return verifier.clone();
        }
    }
}
