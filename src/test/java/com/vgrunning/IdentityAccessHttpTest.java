package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.vgrunning.generated.openapi.server.model.CurrentSession;
import org.vgrunning.generated.openapi.server.model.InvitationAcceptance;
import org.vgrunning.generated.openapi.server.model.InvitationAcceptanceCreation;
import org.vgrunning.generated.openapi.server.model.Runner;
import org.vgrunning.generated.openapi.server.model.RunnerCreation;
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

    @Autowired private JdbcTemplate jdbc;

    @Test
    void createsAnInvitationAndPendingRunnerThroughTheAdministrativeEndpoint() {
        TestRestTemplate client = new TestRestTemplate();
        UUID idempotencyKey = UUID.randomUUID();
        HttpHeaders creationHeaders = administratorHeaders(client, idempotencyKey);
        RunnerCreation request =
                new RunnerCreation("Lucía", "Martín", "lucia.martin@example.invalid", true);

        ResponseEntity<Runner> created =
                client.postForEntity(
                        applicationUrl("/api/runners"),
                        new HttpEntity<>(request, creationHeaders),
                        Runner.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Runner runner = Objects.requireNonNull(created.getBody());
        assertThat(runner.getStatus().getValue()).isEqualTo("pending_activation");
        assertThat(created.getHeaders().getLocation())
                .hasToString("/api/runners/" + runner.getId());
        assertThat(
                        count(
                                "SELECT count(*) FROM runner_management.runner WHERE id = ?",
                                runner.getId()))
                .isOne();
        assertThat(
                        count(
                                """
                                SELECT count(*)
                                  FROM identity_access.access_challenge challenge
                                  JOIN runner_management.runner runner ON runner.account_id = challenge.account_id
                                 WHERE runner.id = ? AND challenge.purpose = 'activation'
                                """,
                                runner.getId()))
                .isOne();
        assertThat(
                        count(
                                """
                                SELECT count(*)
                                  FROM notification_delivery.notification_request request
                                  JOIN runner_management.runner_lifecycle_audit audit
                                    ON audit.correlation_id = request.correlation_id
                                 WHERE audit.runner_id = ?
                                """,
                                runner.getId()))
                .isOne();

        ResponseEntity<Runner> replayed =
                client.postForEntity(
                        applicationUrl("/api/runners"),
                        new HttpEntity<>(request, creationHeaders),
                        Runner.class);
        assertThat(replayed.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(replayed.getBody()).getId()).isEqualTo(runner.getId());
        assertThat(
                        count(
                                "SELECT count(*) FROM runner_management.runner WHERE id = ?",
                                runner.getId()))
                .isOne();
    }

    @Test
    void rollsBackTheIdempotencyReservationWhenTheEmailIsAlreadyReserved() {
        TestRestTemplate client = new TestRestTemplate();
        UUID idempotencyKey = UUID.randomUUID();

        ResponseEntity<String> rejected =
                client.postForEntity(
                        applicationUrl("/api/runners"),
                        new HttpEntity<>(
                                new RunnerCreation(
                                        "Lucía", "Martín", "runner@running-coach.invalid", true),
                                administratorHeaders(client, idempotencyKey)),
                        String.class);

        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT count(*)
                                  FROM runner_management.runner_creation_idempotency
                                 WHERE administrator_account_id = ? AND idempotency_key = ?
                                """,
                                Integer.class,
                                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                                idempotencyKey))
                .isZero();
    }

    @Test
    void acceptsAnInvitationWithoutCreatingASessionAndActivatesItsRunner() {
        String secret = "synthetic-activation-secret";
        PendingInvitation fixture = insertPendingInvitation(secret);
        TestRestTemplate client = new TestRestTemplate();
        ResponseEntity<String> csrf = client.getForEntity(applicationUrl("/"), String.class);
        String csrfCookie =
                cookieValue(
                        csrf.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE), "__Host-pmv_csrf");
        HttpHeaders headers = csrfHeaders(csrfCookie);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<InvitationAcceptance> accepted =
                client.postForEntity(
                        applicationUrl("/api/invitation-acceptances"),
                        new HttpEntity<>(
                                new InvitationAcceptanceCreation(
                                        fixture.challengeId(),
                                        secret,
                                        true,
                                        "synthetic-password-only"),
                                headers),
                        InvitationAcceptance.class);

        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(accepted.getBody()).getStatus().getValue())
                .isEqualTo("accepted");
        assertThat(accepted.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE))
                .noneMatch(cookie -> cookie.startsWith("__Host-pmv_session="));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM identity_access.account WHERE id = ?",
                                String.class,
                                fixture.accountId()))
                .isEqualTo("active");
        assertThat(awaitRunnerActivation(fixture.runnerId())).isEqualTo("active");
        assertThat(
                        count(
                                "SELECT count(*) FROM identity_access.invitation_acceptance WHERE challenge_id = ?",
                                fixture.challengeId()))
                .isOne();
    }

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
        assertThat(rotatedCsrfCookie).isNotEqualTo(initialCsrfCookie);

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
        assertThat(
                        cookieValue(
                                logout.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE),
                                "__Host-pmv_session"))
                .isEmpty();
        assertThat(
                        cookieValue(
                                logout.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE),
                                "__Host-pmv_csrf"))
                .isNotEqualTo(rotatedCsrfCookie);

        ResponseEntity<String> rejected =
                client.exchange(
                        applicationUrl("/api/sessions/current"),
                        HttpMethod.GET,
                        new HttpEntity<>(sessionHeaders),
                        String.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rendersContractualProblemsForMalformedRejectedAndRateLimitedLogins() {
        TestRestTemplate client = new TestRestTemplate();
        ResponseEntity<String> csrf = client.getForEntity(applicationUrl("/"), String.class);
        String csrfCookie =
                cookieValue(
                        csrf.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE), "__Host-pmv_csrf");
        HttpHeaders headers = csrfHeaders(csrfCookie);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> malformed =
                client.postForEntity(
                        applicationUrl("/api/sessions"),
                        new HttpEntity<>("{", headers),
                        String.class);
        assertThat(malformed.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(malformed.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(malformed.getBody()).contains("\"code\":\"invalid_request\"");

        ResponseEntity<String> rejected =
                client.postForEntity(
                        applicationUrl("/api/sessions"),
                        new HttpEntity<>(
                                new SessionCreation(
                                        "runner@running-coach.invalid", "incorrect-password"),
                                headers),
                        String.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rejected.getBody()).contains("\"code\":\"session_creation_rejected\"");

        ResponseEntity<String> rateLimited = null;
        for (int attempt = 0; attempt < 6; attempt++) {
            rateLimited =
                    client.postForEntity(
                            applicationUrl("/api/sessions"),
                            new HttpEntity<>(
                                    new SessionCreation(
                                            "rate-limit@example.invalid", "incorrect-password"),
                                    headers),
                            String.class);
        }
        ResponseEntity<String> finalResponse = Objects.requireNonNull(rateLimited);
        assertThat(finalResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(Long.parseLong(finalResponse.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)))
                .isBetween(1L, 900L);
        assertThat(finalResponse.getBody()).contains("\"code\":\"rate_limit_exceeded\"");
    }

    private HttpHeaders csrfHeaders(String csrfCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "__Host-pmv_csrf=" + csrfCookie);
        headers.add("X-CSRF-TOKEN", csrfCookie);
        return headers;
    }

    private HttpHeaders administratorHeaders(TestRestTemplate client, UUID idempotencyKey) {
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
                                        "administrator@running-coach.invalid",
                                        "synthetic-admin-password-only"),
                                loginHeaders),
                        CurrentSession.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String sessionCookie =
                cookieValue(
                        login.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE),
                        "__Host-pmv_session");
        String csrfCookie =
                cookieValue(
                        login.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE), "__Host-pmv_csrf");
        HttpHeaders headers = csrfHeaders(csrfCookie);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(
                HttpHeaders.COOKIE,
                "__Host-pmv_session=" + sessionCookie + "; __Host-pmv_csrf=" + csrfCookie);
        headers.set("Idempotency-Key", idempotencyKey.toString());
        return headers;
    }

    private static String cookieValue(List<String> setCookies, String name) {
        return setCookies.stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .reduce((ignored, newest) -> newest)
                .map(cookie -> cookie.substring(name.length() + 1, cookie.indexOf(';')))
                .orElseThrow(() -> new AssertionError("No se emitió la cookie " + name));
    }

    private int count(String query, UUID id) {
        return Objects.requireNonNull(jdbc.queryForObject(query, Integer.class, id));
    }

    private String awaitRunnerActivation(UUID runnerId) {
        String status = runnerStatus(runnerId);
        for (int attempt = 0; attempt < 20 && !"active".equals(status); attempt++) {
            LockSupport.parkNanos(Duration.ofMillis(100).toNanos());
            status = runnerStatus(runnerId);
        }
        return status;
    }

    private String runnerStatus(UUID runnerId) {
        return Objects.requireNonNull(
                jdbc.queryForObject(
                        "SELECT status FROM runner_management.runner WHERE id = ?",
                        String.class,
                        runnerId));
    }

    private PendingInvitation insertPendingInvitation(String secret) {
        UUID accountId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        UUID runnerId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO identity_access.account
                    (id, role, status, password_hash, created_at, updated_at, status_changed_at, version)
                VALUES (?, 'corredor', 'pending_activation', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP, 0)
                """,
                accountId);
        jdbc.update(
                """
                INSERT INTO identity_access.account_email
                    (id, account_id, presentation_email, canonical_email, usage, created_at, updated_at)
                VALUES (?, ?, 'activation@example.invalid', ?, 'current', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(),
                accountId,
                "activation-" + accountId + "@example.invalid");
        jdbc.update(
                """
                INSERT INTO identity_access.access_challenge
                    (id, account_id, purpose, generation, verifier_sha256, created_at, expires_at)
                VALUES (?, ?, 'activation', 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day')
                """,
                challengeId,
                accountId,
                sha256(secret));
        jdbc.update(
                """
                INSERT INTO runner_management.runner
                    (id, account_id, given_name, family_name, status, created_at, pending_activation_expires_at)
                VALUES (?, ?, 'Lucía', 'Martín', 'pending_activation', CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP + INTERVAL '1 day')
                """,
                runnerId,
                accountId);
        return new PendingInvitation(accountId, challengeId, runnerId);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private record PendingInvitation(UUID accountId, UUID challengeId, UUID runnerId) {}

    private String applicationUrl(String path) {
        return "http://localhost:" + applicationPort + path;
    }
}
