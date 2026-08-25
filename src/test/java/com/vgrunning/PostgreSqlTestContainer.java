package com.vgrunning;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Crea PostgreSQL 18 efímero con el mismo digest utilizado por codegen y Compose. */
final class PostgreSqlTestContainer {

    private static final DockerImageName IMAGE = DockerImageName
        .parse("postgres@sha256:1957b2ff3137e4ef7f3bc813e74fff50b1e1ffddc85c8b9d6f14ade972be8687")
        .asCompatibleSubstituteFor("postgres");

    private PostgreSqlTestContainer() {
    }

    /** Crea un contenedor tipado para que cada clase de prueba disponga de una base aislada. */
    static PostgreSQLContainer create() {
        return new PostgreSQLContainer(IMAGE)
            .withDatabaseName("running_coach_test")
            .withUsername("running_coach")
            .withPassword("running_coach");
    }

    /** Publica la conexión del contenedor sin versionar ni compartir credenciales locales. */
    static void registerProperties(DynamicPropertyRegistry registry, PostgreSQLContainer postgres) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
