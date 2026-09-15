package com.vgrunning.identityaccess.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Origen público canónico de la SPA, propiedad del despliegue y no del módulo. */
@ConfigurationProperties(prefix = "pmv.web")
public record WebOriginProperties(String publicOrigin) {}
