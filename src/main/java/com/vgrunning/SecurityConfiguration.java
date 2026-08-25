package com.vgrunning;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Aplica una política HTTP cerrada mientras {@code identity-access} no proporcione autenticación de producto.
 *
 * <p>Desactiva los mecanismos de acceso que Spring Security habilitaría para una aplicación interactiva y
 * deniega cualquier ruta salvo las probes técnicas de liveness y readiness. Las propiedades de Spring Boot
 * sirven para configurar el puerto, la exposición y el contenido de Actuator, pero no expresan esta política
 * de autorización por ruta; por eso se declara explícitamente mediante el DSL de Spring Security.</p>
 */
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
