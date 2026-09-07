package com.vgrunning.runnerportal.infrastructure.configuration.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configura la entrega conjunta de la SPA sin exponer rutas técnicas como rutas de cliente. */
@Configuration(proxyBeanMethods = false)
class SpaWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false)
                .addResolver(new SpaResourceResolver());
    }
}
