package com.vgrunning.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Aplica una política HTTP cerrada mientras {@code identity-access} no proporcione autenticación de
 * producto.
 *
 * <p>Desactiva los mecanismos de acceso interactivo, permite la lectura del shell público de la SPA
 * y las probes técnicas, y mantiene cerrados API, Actuator y métodos inseguros.
 */
@Configuration
class SecurityConfiguration {

    private static final String[] TECHNICAL_PROBES = {
        "/actuator/health/liveness", "/actuator/health/readiness"
    };

    private static final String[] RESTRICTED_ENDPOINTS = {
        "/actuator", "/actuator/**", "/api", "/api/**", "/error"
    };

    private static final String PUBLIC_SPA_PATH = "/**";

    @Bean
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) {
        try {
            return http.httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .logout(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(this::configureAuthorizationRules)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Error al configurar la seguridad HTTP", ex);
        }
    }

    private void configureAuthorizationRules(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
                    auth) {
        auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                .permitAll()
                .requestMatchers(TECHNICAL_PROBES)
                .permitAll()
                .requestMatchers(RESTRICTED_ENDPOINTS)
                .denyAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_SPA_PATH)
                .permitAll()
                .requestMatchers(HttpMethod.HEAD, PUBLIC_SPA_PATH)
                .permitAll()
                .anyRequest()
                .denyAll();
    }
}
