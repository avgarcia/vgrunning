package com.vgrunning.identityaccess.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración local de la clave AEAD de las invitaciones; no admite valores predeterminados. */
@ConfigurationProperties(prefix = "pmv.identity.invitation-encryption")
public record InvitationEncryptionProperties(String key) {}
