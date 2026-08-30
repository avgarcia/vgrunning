package com.vgrunning.identityaccess.adapter.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/** Parámetros mínimos aprobados para hashes Argon2id de cuentas locales. */
@Configuration
class IdentityAccessCryptoConfiguration {

    @Bean
    Argon2PasswordEncoder identityAccessPasswordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }
}
