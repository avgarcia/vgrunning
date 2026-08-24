package com.vgrunning;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/** Política temporal cerrada hasta que {@code identity-access} proporcione autenticación de producto. */
@Configuration
class SecurityConfiguration {

    @Bean
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .anyRequest().denyAll()
            )
            .build();
    }
}
