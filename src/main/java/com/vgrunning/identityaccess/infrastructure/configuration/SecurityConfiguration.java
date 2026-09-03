package com.vgrunning.identityaccess.infrastructure.configuration;

import com.vgrunning.identityaccess.infrastructure.security.AuthenticationRequiredException;
import com.vgrunning.identityaccess.infrastructure.security.CsrfValidationException;
import com.vgrunning.identityaccess.infrastructure.security.OriginValidationFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Configuración transversal de Spring Security para la aplicación servlet. */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /**
     * Configura la frontera HTTP de Spring Security: deshabilita Basic, formulario y logout
     * implícito; persiste explícitamente el contexto en Spring Session; exige CSRF y Origin para
     * mutaciones; permite únicamente las operaciones públicas de login y salud; y traduce los
     * rechazos de autenticación y CSRF al formato Problem Details del contrato.
     */
    @Bean
    @SuppressFBWarnings(
            value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION",
            justification =
                    "HttpSecurity.build() declara Exception sin una alternativa más concreta.")
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContexts,
            OriginValidationFilter originValidation,
            CsrfTokenRepository csrfTokens,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            HandlerExceptionResolver handlerExceptionResolver)
            throws Exception {
        return http.httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .securityContext(
                        context ->
                                context.securityContextRepository(securityContexts)
                                        .requireExplicitSave(true))
                .csrf(
                        csrf ->
                                csrf.spa()
                                        .csrfTokenRepository(csrfTokens)
                                        .sessionAuthenticationStrategy(
                                                sessionAuthenticationStrategy))
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(
                                                (request, response, exception) -> {
                                                    if (!"/api/sessions/current"
                                                            .equals(request.getServletPath())) {
                                                        response.sendError(
                                                                HttpServletResponse.SC_FORBIDDEN);
                                                        return;
                                                    }
                                                    handlerExceptionResolver.resolveException(
                                                            request,
                                                            response,
                                                            null,
                                                            new AuthenticationRequiredException());
                                                })
                                        .accessDeniedHandler(
                                                accessDeniedHandler(handlerExceptionResolver)))
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers(
                                                "/actuator/health/liveness",
                                                "/actuator/health/readiness")
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
                .build();
    }

    /** Configura la cookie CSRF que consume la SPA en el mismo origen. */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository tokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokens.setCookieName("__Host-pmv_csrf");
        tokens.setHeaderName("X-CSRF-TOKEN");
        tokens.setCookiePath("/");
        tokens.setCookieCustomizer(cookie -> cookie.secure(true).sameSite("Lax"));
        return tokens;
    }

    /** Traduce rechazos CSRF del filtro de seguridad a la respuesta HTTP acordada. */
    private static AccessDeniedHandler accessDeniedHandler(
            HandlerExceptionResolver handlerExceptionResolver) {
        return (request, response, exception) ->
                handlerExceptionResolver.resolveException(
                        request, response, null, new CsrfValidationException());
    }
}
