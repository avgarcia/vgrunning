package com.vgrunning.identityaccess.infrastructure.configuration;

import com.vgrunning.identityaccess.infrastructure.security.AuthenticationRequiredException;
import com.vgrunning.identityaccess.infrastructure.security.CsrfValidationException;
import com.vgrunning.identityaccess.infrastructure.security.OriginValidationFilter;
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
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Configuración transversal de Spring Security para la aplicación servlet. */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    @SuppressFBWarnings(
            value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION",
            justification =
                    "HttpSecurity.build declara Exception y Spring invoca este método de configuración.")
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository opaqueSessions,
            OriginValidationFilter originValidation,
            CsrfTokenRepository csrfTokens,
            HandlerExceptionResolver handlerExceptionResolver)
            throws Exception {
        return http.httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(
                        context ->
                                context.securityContextRepository(opaqueSessions)
                                        .requireExplicitSave(true))
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokens))
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

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository tokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokens.setCookieName("__Host-pmv_csrf");
        tokens.setHeaderName("X-CSRF-TOKEN");
        tokens.setCookiePath("/");
        tokens.setCookieCustomizer(cookie -> cookie.secure(true).sameSite("Lax"));
        return tokens;
    }

    private static AccessDeniedHandler accessDeniedHandler(
            HandlerExceptionResolver handlerExceptionResolver) {
        return (request, response, exception) ->
                handlerExceptionResolver.resolveException(
                        request, response, null, new CsrfValidationException());
    }
}
