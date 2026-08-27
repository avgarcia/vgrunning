package com.vgrunning;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Aplica una política HTTP cerrada mientras {@code identity-access} no proporcione autenticación de
 * producto.
 *
 * <p>Desactiva los mecanismos de acceso que Spring Security habilitaría para una aplicación
 * interactiva, permite únicamente la lectura del shell público y las probes técnicas, y mantiene
 * cerrados API, Actuator y métodos inseguros. Las propiedades de Spring Boot configuran el puerto,
 * la exposición y el contenido de Actuator, pero no expresan esta política por ruta; por eso se
 * declara mediante el DSL de Spring Security.
 */
@Configuration
class SecurityConfiguration {

    @Bean
    @SuppressFBWarnings(
            value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION",
            justification =
                    "HttpSecurity.build declara Exception y Spring invoca este método de configuración.")
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers(
                                                "/actuator/health/liveness",
                                                "/actuator/health/readiness")
                                        .permitAll()
                                        .requestMatchers(
                                                "/actuator",
                                                "/actuator/**",
                                                "/api",
                                                "/api/**",
                                                "/error")
                                        .denyAll()
                                        .requestMatchers(HttpMethod.GET, "/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.HEAD, "/**")
                                        .permitAll()
                                        .anyRequest()
                                        .denyAll())
                .build();
    }
}
