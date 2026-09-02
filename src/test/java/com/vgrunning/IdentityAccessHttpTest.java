package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.vgrunning.generated.openapi.server.model.CurrentSession;
import org.vgrunning.generated.openapi.server.model.SessionCreation;

/** Recorre el contrato HTTP de sesión exclusivamente con las cuentas sintéticas opt-in. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
@ActiveProfiles({"local", "synthetic-accounts"})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IdentityAccessHttpTest {

    @Container private static final PostgreSQLContainer POSTGRES = PostgreSqlTestContainer.create();

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainer.registerProperties(registry, POSTGRES);
    }

    @LocalServerPort private int applicationPort;

    @Test
    void createsQueriesAndInvalidatesASpringSession() {
        TestRestTemplate client = new TestRestTemplate();
        ResponseEntity<String> csrf = client.getForEntity(applicationUrl("/"), String.class);
        String initialCsrfCookie =
                cookieValue(
                        csrf.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE), "__Host-pmv_csrf");

        HttpHeaders loginHeaders = csrfHeaders(initialCsrfCookie);
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<CurrentSession> login =
                client.postForEntity(
                        applicationUrl("/api/sessions"),
                        new HttpEntity<>(
                                new SessionCreation(
                                        "runner@running-coach.invalid",
                                        "synthetic-runner-password-only"),
                                loginHeaders),
                        CurrentSession.class);
        String sessionCookie =
                cookieValue(
                        login.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE),
                        "__Host-pmv_session");
        String rotatedCsrfCookie =
                cookieValue(
                        login.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE), "__Host-pmv_csrf");

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CurrentSession loginBody = Objects.requireNonNull(login.getBody());
        assertThat(loginBody.getAccountStatus().getValue()).isEqualTo("active");
        assertThat(login.getHeaders().getLocation()).hasToString("/api/sessions/current");

        HttpHeaders sessionHeaders = new HttpHeaders();
        sessionHeaders.add(HttpHeaders.COOKIE, "__Host-pmv_session=" + sessionCookie);
        ResponseEntity<CurrentSession> current =
                client.exchange(
                        applicationUrl("/api/sessions/current"),
                        HttpMethod.GET,
                        new HttpEntity<>(sessionHeaders),
                        CurrentSession.class);
        assertThat(current.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders logoutHeaders = csrfHeaders(rotatedCsrfCookie);
        logoutHeaders.set(
                HttpHeaders.COOKIE,
                "__Host-pmv_session=" + sessionCookie + "; __Host-pmv_csrf=" + rotatedCsrfCookie);
        ResponseEntity<Void> logout =
                client.exchange(
                        applicationUrl("/api/sessions/current"),
                        HttpMethod.DELETE,
                        new HttpEntity<>(logoutHeaders),
                        Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> rejected =
                client.exchange(
                        applicationUrl("/api/sessions/current"),
                        HttpMethod.GET,
                        new HttpEntity<>(sessionHeaders),
                        String.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpHeaders csrfHeaders(String csrfCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "__Host-pmv_csrf=" + csrfCookie);
        headers.add("X-CSRF-TOKEN", csrfCookie);
        return headers;
    }

    private static String cookieValue(List<String> setCookies, String name) {
        return setCookies.stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .reduce((ignored, newest) -> newest)
                .map(cookie -> cookie.substring(name.length() + 1, cookie.indexOf(';')))
                .orElseThrow(() -> new AssertionError("No se emitió la cookie " + name));
    }

    private String applicationUrl(String path) {
        return "http://localhost:" + applicationPort + path;
    }
}
