package com.vgrunning;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Un único PostgreSQL 18 para toda la suite de integración en vez de uno por clase: arrancar y
 * migrar un contenedor por clase encadenaba varios minutos de Testcontainers en cada build.
 */
final class PostgreSqlTestContainer {

    private static final DockerImageName IMAGE =
            DockerImageName.parse(
                            "postgres@sha256:1957b2ff3137e4ef7f3bc813e74fff50b1e1ffddc85c8b9d6f14ade972be8687")
                    .asCompatibleSubstituteFor("postgres");

    private static final PostgreSQLContainer INSTANCE = start();

    private PostgreSqlTestContainer() {}

    private static PostgreSQLContainer start() {
        PostgreSQLContainer container =
                new PostgreSQLContainer(IMAGE)
                        .withDatabaseName("running_coach_test")
                        .withUsername("running_coach")
                        .withPassword("running_coach");
        container.start();
        return container;
    }

    /** Publica la conexión del único contenedor compartido por toda la suite de integración. */
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", INSTANCE::getUsername);
        registry.add("spring.datasource.password", INSTANCE::getPassword);
    }

    /**
     * Vacía los datos de todas las tablas de aplicación entre clases de prueba, preservando el
     * esquema y el historial de Flyway para que ningún contexto vuelva a migrar sobre el mismo
     * contenedor.
     *
     * <p>Invariante que debe respetar quien la llame: ninguna clase de prueba puede depender de
     * filas creadas por otra. En particular, {@code IdentityAccessHttpTest} no debe llamarla: sus
     * cuentas sintéticas las siembra {@code SyntheticAccountsBootstrap} una sola vez, al crearse su
     * contexto de Spring, así que un truncado posterior las borraría sin que nada vuelva a
     * insertarlas mientras ese contexto siga en la caché de pruebas.
     */
    static void truncateApplicationData() {
        try (Connection connection =
                DriverManager.getConnection(
                        INSTANCE.getJdbcUrl(), INSTANCE.getUsername(), INSTANCE.getPassword())) {
            String tablesToTruncate = applicationTables(connection);
            if (!tablesToTruncate.isEmpty()) {
                try (Statement truncate = connection.createStatement()) {
                    truncate.execute(
                            "TRUNCATE TABLE " + tablesToTruncate + " RESTART IDENTITY CASCADE");
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String applicationTables(Connection connection) throws SQLException {
        StringBuilder tables = new StringBuilder();
        try (Statement listing = connection.createStatement();
                ResultSet rows =
                        listing.executeQuery(
                                """
                                SELECT schemaname, tablename FROM pg_tables
                                 WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
                                   AND tablename <> 'flyway_schema_history'
                                """)) {
            while (rows.next()) {
                if (!tables.isEmpty()) {
                    tables.append(", ");
                }
                tables.append('"')
                        .append(rows.getString("schemaname"))
                        .append("\".\"")
                        .append(rows.getString("tablename"))
                        .append('"');
            }
        }
        return tables.toString();
    }
}
