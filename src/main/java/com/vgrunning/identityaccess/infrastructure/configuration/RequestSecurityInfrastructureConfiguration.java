package com.vgrunning.identityaccess.infrastructure.configuration;

import com.vgrunning.identityaccess.infrastructure.security.OriginValidationFilter;
import com.vgrunning.identityaccess.infrastructure.security.ratelimit.LoginRateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Compone las protecciones técnicas aplicadas antes de ejecutar una solicitud de sesión. */
@Configuration(proxyBeanMethods = false)
public class RequestSecurityInfrastructureConfiguration {

    /** Crea el limitador local de intentos de acceso y publica sus métricas acotadas. */
    @Bean
    LoginRateLimiter loginRateLimiter(MeterRegistry meterRegistry) {
        return new LoginRateLimiter(meterRegistry);
    }

    /** Rechaza Origin declarados que no correspondan a la petición protegida. */
    @Bean
    OriginValidationFilter originValidationFilter(
            HandlerExceptionResolver handlerExceptionResolver) {
        return new OriginValidationFilter(handlerExceptionResolver);
    }
}
