package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Comprueba la frontera HTTP con PostgreSQL real. {@code DirtiesContext} expulsa el contexto y su
 * pool de la caché al acabar la clase; así ninguna prueba posterior reutiliza una conexión al
 * contenedor estático, que Testcontainers destruye al finalizar esta clase.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(SecurityAndManagementEndpointTest.SyntheticErrorEndpoint.class)
class SecurityAndManagementEndpointTest {

    private static final Pattern SCRIPT_ASSET = Pattern.compile("src=\"(/assets/[^\"]+\\.js)\"");

    @Container private static final PostgreSQLContainer POSTGRES = PostgreSqlTestContainer.create();

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainer.registerProperties(registry, POSTGRES);
    }

    @LocalServerPort private int applicationPort;

    @LocalManagementPort private int managementPort;

    @Test
    void servesTheSpaAtTheRootAndForClientRoutes() {
        TestRestTemplate client = new TestRestTemplate();

        ResponseEntity<String> root = client.getForEntity(applicationUrl("/"), String.class);
        ResponseEntity<String> clientRoute =
                client.getForEntity(applicationUrl("/synthetic/client-route"), String.class);

        assertThat(root.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(root.getBody()).contains("<div id=\"root\"></div>");
        assertThat(clientRoute.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(clientRoute.getBody()).isEqualTo(root.getBody());
    }

    @Test
    void servesGeneratedAssetsWithoutFallingBackForMissingAssets() {
        TestRestTemplate client = new TestRestTemplate();
        String index = client.getForObject(applicationUrl("/"), String.class);
        Matcher matcher = SCRIPT_ASSET.matcher(index);

        assertThat(matcher.find()).isTrue();
        ResponseEntity<String> asset =
                client.getForEntity(applicationUrl(matcher.group(1)), String.class);
        ResponseEntity<String> missingAsset =
                client.getForEntity(applicationUrl("/assets/missing-script.js"), String.class);

        assertThat(asset.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asset.getHeaders().getContentType()).hasToString("text/javascript");
        assertThat(asset.getBody()).isNotBlank();
        assertThat(missingAsset.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missingAsset.getBody()).doesNotContain("<div id=\"root\"></div>");
    }

    @Test
    void keepsApiErrorsAndUnsafeRequestsOutsideTheSpaFallback() {
        TestRestTemplate client = new TestRestTemplate();

        ResponseEntity<String> api =
                client.getForEntity(applicationUrl("/api/not-found"), String.class);
        ResponseEntity<String> error =
                client.getForEntity(applicationUrl("/synthetic-error"), String.class);
        ResponseEntity<String> post =
                client.postForEntity(applicationUrl("/synthetic/client-route"), null, String.class);

        assertThat(api.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(error.getBody()).doesNotContain("<div id=\"root\"></div>");
        assertThat(post.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void doesNotEnableCorsForThePublicShell() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://example.invalid");

        ResponseEntity<String> response =
                new TestRestTemplate()
                        .exchange(
                                applicationUrl("/"),
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void exposesOnlyTheLivenessAndReadinessProbesWithoutAuthentication() {
        TestRestTemplate managementClient = new TestRestTemplate();

        assertThat(
                        managementClient
                                .getForEntity(
                                        managementUrl("/actuator/health/liveness"), String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(
                        managementClient
                                .getForEntity(
                                        managementUrl("/actuator/health/readiness"), String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(
                        managementClient
                                .getForEntity(managementUrl("/actuator/health"), String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String managementUrl(String path) {
        return "http://localhost:" + managementPort + path;
    }

    private String applicationUrl(String path) {
        return "http://localhost:" + applicationPort + path;
    }

    @RestController
    static class SyntheticErrorEndpoint {

        @GetMapping("/synthetic-error")
        void fail() {
            throw new IllegalStateException("synthetic failure");
        }
    }
}
