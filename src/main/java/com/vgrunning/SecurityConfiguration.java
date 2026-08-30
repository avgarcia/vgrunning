package com.vgrunning;

import com.vgrunning.identityaccess.adapter.security.AuthenticationRequiredException;
import com.vgrunning.identityaccess.adapter.security.CsrfValidationException;
import com.vgrunning.identityaccess.adapter.security.OpaqueSessionAuthenticationFilter;
import com.vgrunning.identityaccess.adapter.security.OriginValidationFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Aplica una política HTTP cerrada y abre únicamente las cuatro operaciones de identidad ya
 * declaradas en OpenAPI.
 *
 * <p>Desactiva los mecanismos de acceso que Spring Security habilitaría para una aplicación
 * interactiva, permite únicamente la lectura del shell público, las probes técnicas y los contratos
 * de sesión aprobados. El resto de API, Actuator y métodos inseguros continúa cerrado.
 */
@Configuration
class SecurityConfiguration {

    @Bean
    @SuppressFBWarnings(
            value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION",
            justification =
                    "HttpSecurity.build declara Exception y Spring invoca este método de configuración.")
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            OpaqueSessionAuthenticationFilter opaqueSessions,
            OriginValidationFilter originValidation,
            CsrfTokenRepository csrfTokens,
            HandlerExceptionResolver handlerExceptionResolver)
            throws Exception {
        return http.httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(
                        csrf ->
                                csrf.csrfTokenRepository(csrfTokens)
                                        .csrfTokenRequestHandler(
                                                new CsrfTokenRequestAttributeHandler()))
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(
                                                (request, response, exception) ->
                                                        handlerExceptionResolver.resolveException(
                                                                request,
                                                                response,
                                                                null,
                                                                new AuthenticationRequiredException()))
                                        .accessDeniedHandler(accessDeniedHandler(handlerExceptionResolver)))
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers(
                                                "/actuator/health/liveness",
                                                "/actuator/health/readiness")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/csrf-tokens/current")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/sessions")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/sessions/current")
                                        .authenticated()
                                        .requestMatchers(HttpMethod.DELETE, "/api/sessions/current")
                                        .authenticated()
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
                .addFilterBefore(
                        originValidation, org.springframework.security.web.csrf.CsrfFilter.class)
                .addFilterBefore(opaqueSessions, AnonymousAuthenticationFilter.class)
                .build();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository tokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokens.setCookieName("__Host-pmv_csrf");
        tokens.setHeaderName("X-CSRF-TOKEN");
        tokens.setCookiePath("/");
        tokens.setCookieCustomizer(cookie -> cookie.secure(true).sameSite("Lax"));
        return tokens;
    }

    private static AccessDeniedHandler accessDeniedHandler(HandlerExceptionResolver handlerExceptionResolver) {
        return (request, response, exception) ->
                handlerExceptionResolver.resolveException(
                        request, response, null, new CsrfValidationException());
    }
}
