package com.vgrunning.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

class Argon2PasswordHasherTest {

    @Test
    void neverAuthenticatesWithTheDummyHash() {
        Argon2PasswordHasher hasher =
                new Argon2PasswordHasher(new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2));

        assertThat(hasher.matchesForAuthentication("not-a-real-password", Optional.empty()))
                .isFalse();
    }
}
