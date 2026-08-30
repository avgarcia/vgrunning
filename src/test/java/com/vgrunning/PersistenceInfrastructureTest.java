package com.vgrunning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.vgrunning.generated.jooq.platform.tables.EventPublication.EVENT_PUBLICATION;

import com.zaxxer.hikari.HikariDataSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistenceInfrastructureTest {

    private static final Set<String> APPLICATION_SCHEMAS =
            Set.of(
                    "identity_access",
                    "runner_management",
                    "classification_segmentation",
                    "planning",
                    "publication",
                    "notification_delivery",
                    "tracking_review",
                    "platform");

    private static final List<String> GENERATED_SCHEMA_CLASSES =
            List.of(
                    "org.vgrunning.generated.jooq.identity_access.IdentityAccess",
                    "org.vgrunning.generated.jooq.runner_management.RunnerManagement",
                    "org.vgrunning.generated.jooq.classification_segmentation.ClassificationSegmentation",
                    "org.vgrunning.generated.jooq.planning.Planning",
                    "org.vgrunning.generated.jooq.publication.Publication",
                    "org.vgrunning.generated.jooq.notification_delivery.NotificationDelivery",
                    "org.vgrunning.generated.jooq.tracking_review.TrackingReview",
                    "org.vgrunning.generated.jooq.platform.Platform");

    @Container private static final PostgreSQLContainer POSTGRES = PostgreSqlTestContainer.create();

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        PostgreSqlTestContainer.registerProperties(registry, POSTGRES);
    }

    @Autowired private DataSource dataSource;

    @Autowired private DSLContext jooq;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private Environment environment;

    @Test
    void runsPostgreSql18WithTheExactApplicationSchemas() {
        Integer serverVersionNumber = jdbc.queryForObject("SHOW server_version_num", Integer.class);
        Set<String> schemas =
                Set.copyOf(
                        jdbc.queryForList(
                                "SELECT schema_name FROM information_schema.schemata",
                                String.class));
        Set<String> applicationSchemas =
                schemas.stream().filter(APPLICATION_SCHEMAS::contains).collect(Collectors.toSet());

        assertThat(serverVersionNumber).isBetween(180000, 189999);
        assertThat(applicationSchemas).containsExactlyInAnyOrderElementsOf(APPLICATION_SCHEMAS);
        assertThat(schemas).doesNotContain("runner_portal", "runner-portal");
    }

    @Test
    void ownsOneFlywayHistoryAndTheCurrentModulithRegistryInPlatform() {
        List<String> flywayHistorySchemas =
                jdbc.queryForList(
                        """
            SELECT table_schema
              FROM information_schema.tables
             WHERE table_name = 'flyway_schema_history'
            """,
                        String.class);
        Set<String> eventColumns =
                Set.copyOf(
                        jdbc.queryForList(
                                """
            SELECT column_name
              FROM information_schema.columns
             WHERE table_schema = 'platform'
               AND table_name = 'event_publication'
            """,
                                String.class));
        Set<String> eventIndexes =
                Set.copyOf(
                        jdbc.queryForList(
                                """
            SELECT indexname
              FROM pg_indexes
             WHERE schemaname = 'platform'
               AND tablename = 'event_publication'
            """,
                                String.class));

        assertThat(flywayHistorySchemas).containsExactly("platform");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM platform.flyway_schema_history WHERE success AND version IS NOT NULL",
                                Integer.class))
                .isEqualTo(3);
        assertThat(eventColumns)
                .containsExactlyInAnyOrder(
                        "id",
                        "listener_id",
                        "event_type",
                        "serialized_event",
                        "publication_date",
                        "completion_date",
                        "status",
                        "completion_attempts",
                        "last_resubmission_date");
        assertThat(eventIndexes)
                .containsExactlyInAnyOrder(
                        "event_publication_pkey",
                        "event_publication_serialized_event_hash_idx",
                        "event_publication_by_completion_date_idx");
        assertThat(
                        jdbc.queryForObject(
                                """
            SELECT count(*)
              FROM information_schema.tables
             WHERE table_schema = 'platform'
               AND table_name = 'event_publication_archive'
            """,
                                Integer.class))
                .isZero();
    }

    @Test
    void createsIdentityAccessTablesWithSemanticTimesAndPartialIndexes() {
        Set<String> identityTables =
                Set.copyOf(
                        jdbc.queryForList(
                                """
                                SELECT table_name FROM information_schema.tables
                                 WHERE table_schema = 'identity_access' AND table_type = 'BASE TABLE'
                                """,
                                String.class));
        Set<String> accountColumns =
                Set.copyOf(
                        jdbc.queryForList(
                                """
                                SELECT column_name FROM information_schema.columns
                                 WHERE table_schema = 'identity_access' AND table_name = 'account'
                                """,
                                String.class));
        Set<String> timestampColumns =
                Set.copyOf(
                        jdbc.queryForList(
                                """
                                SELECT table_name || '.' || column_name
                                  FROM information_schema.columns
                                 WHERE table_schema = 'identity_access'
                                   AND data_type = 'timestamp with time zone'
                                """,
                                String.class));
        Set<String> indexes =
                Set.copyOf(
                        jdbc.queryForList(
                                """
                                SELECT indexname FROM pg_indexes
                                 WHERE schemaname = 'identity_access'
                                """,
                                String.class));

        assertThat(identityTables)
                .containsExactlyInAnyOrder(
                        "account",
                        "account_email",
                        "access_session",
                        "auth_rate_limit_bucket",
                        "security_event");
        assertThat(accountColumns)
                .contains(
                        "created_at",
                        "updated_at",
                        "status_changed_at",
                        "password_changed_at",
                        "version");
        assertThat(timestampColumns)
                .contains(
                        "account.created_at",
                        "account.updated_at",
                        "account.status_changed_at",
                        "account_email.confirmed_at",
                        "account_email.expires_at",
                        "account_email.released_at",
                        "access_session.absolute_expires_at",
                        "access_session.revoked_at",
                        "auth_rate_limit_bucket.window_started_at",
                        "auth_rate_limit_bucket.window_ends_at",
                        "security_event.occurred_at",
                        "security_event.retention_until");
        assertThat(indexes)
                .contains(
                        "account_email_live_canonical_usage_key",
                        "account_email_account_usage_reservation_key",
                        "access_session_active_account_idx",
                        "access_session_verifier_sha256_key");
    }

    @Test
    void disablesModulithSchemaInitializationAndCompilesEveryGeneratedSchema() {
        assertThat(
                        environment.getProperty(
                                "spring.modulith.events.jdbc.schema-initialization.enabled",
                                Boolean.class))
                .isFalse();
        assertThat(environment.getProperty("spring.modulith.events.jdbc.schema"))
                .isEqualTo("platform");
        assertThat(GENERATED_SCHEMA_CLASSES)
                .allSatisfy(className -> assertThatCodegenClassExists(className));
    }

    @Test
    void sharesTheDataSourceForJooqJdbcCommitAndRollback() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        assertThat(transactionManager).isInstanceOf(JdbcTransactionManager.class);
        assertThat(jooq.dialect()).isEqualTo(SQLDialect.POSTGRES);

        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        UUID committedId = UUID.randomUUID();
        transactions.executeWithoutResult(status -> insertSyntheticPublication(committedId));
        assertThat(countPublication(committedId)).isOne();

        UUID rolledBackId = UUID.randomUUID();
        transactions.executeWithoutResult(
                status -> {
                    insertSyntheticPublication(rolledBackId);
                    assertThat(countPublication(rolledBackId)).isOne();
                    status.setRollbackOnly();
                });
        assertThat(countPublication(rolledBackId)).isZero();

        jdbc.update("DELETE FROM platform.event_publication WHERE id = ?", committedId);
    }

    private void insertSyntheticPublication(UUID id) {
        jooq.insertInto(EVENT_PUBLICATION)
                .set(EVENT_PUBLICATION.ID, id)
                .set(EVENT_PUBLICATION.LISTENER_ID, "synthetic-listener")
                .set(EVENT_PUBLICATION.EVENT_TYPE, "synthetic-event")
                .set(EVENT_PUBLICATION.SERIALIZED_EVENT, "{\"kind\":\"synthetic\"}")
                .set(EVENT_PUBLICATION.PUBLICATION_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();
    }

    private int countPublication(UUID id) {
        return Objects.requireNonNull(
                jdbc.queryForObject(
                        "SELECT count(*) FROM platform.event_publication WHERE id = ?",
                        Integer.class,
                        id),
                "La consulta de conteo debe devolver un valor.");
    }

    private static void assertThatCodegenClassExists(String className) {
        try {
            assertThat(Class.forName(className)).isNotNull();
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("No se generó el esquema jOOQ " + className, exception);
        }
    }
}
