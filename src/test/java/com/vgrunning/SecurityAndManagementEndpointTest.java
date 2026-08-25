package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Comprueba la frontera HTTP con PostgreSQL real. {@code DirtiesContext} cierra el contexto y su
 * pool antes de que Testcontainers destruya el contenedor estático de esta clase; así el contexto
 * no queda reutilizable con una conexión ya cerrada en otra prueba.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityAndManagementEndpointTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = PostgreSqlTestContainer.create();

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainer.registerProperties(registry, POSTGRES);
    }

    @LocalServerPort
    private int applicationPort;

    @LocalManagementPort
    private int managementPort;

    @Test
    void deniesEveryApplicationRouteByDefault() {
        ResponseEntity<String> response = new TestRestTemplate().getForEntity(
            applicationUrl("/not-an-application-endpoint"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void exposesOnlyTheLivenessAndReadinessProbesWithoutAuthentication() {
        TestRestTemplate managementClient = new TestRestTemplate();

        assertThat(managementClient.getForEntity(managementUrl("/actuator/health/liveness"), String.class).getStatusCode())
            .isEqualTo(HttpStatus.OK);
        assertThat(managementClient.getForEntity(managementUrl("/actuator/health/readiness"), String.class).getStatusCode())
            .isEqualTo(HttpStatus.OK);
        assertThat(managementClient.getForEntity(managementUrl("/actuator/health"), String.class).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String managementUrl(String path) {
        return "http://localhost:" + managementPort + path;
    }

    private String applicationUrl(String path) {
        return "http://localhost:" + applicationPort + path;
    }
}
