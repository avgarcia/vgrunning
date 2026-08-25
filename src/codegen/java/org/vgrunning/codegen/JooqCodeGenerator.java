package org.vgrunning.codegen;

import java.nio.file.Path;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.SchemaMappingType;
import org.jooq.meta.jaxb.Target;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Genera los tipos jOOQ desde la misma historia Flyway que ejecuta la aplicación.
 *
 * <p>Se ejecuta exclusivamente durante el build, en desarrollo o CI, contra PostgreSQL efímero.
 * No forma parte del {@code bootJar}, no arranca contenedores en el entorno desplegado y no se
 * ejecuta al iniciar la aplicación.
 */
public final class JooqCodeGenerator {

    private static final String POSTGRES_IMAGE =
        "postgres@sha256:1957b2ff3137e4ef7f3bc813e74fff50b1e1ffddc85c8b9d6f14ade972be8687";
    private static final List<String> SCHEMAS = List.of(
        "identity_access",
        "runner_management",
        "classification_segmentation",
        "planning",
        "publication",
        "notification_delivery",
        "tracking_review",
        "platform"
    );

    private JooqCodeGenerator() {
    }

    /** Arranca PostgreSQL, migra, genera las fuentes y siempre destruye el contenedor. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Se esperaba el directorio de salida como único argumento.");
        }

        Path targetDirectory = Path.of(args[0]).toAbsolutePath().normalize();
        DockerImageName image = DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres");

        try (PostgreSQLContainer postgres = new PostgreSQLContainer(image)
            .withDatabaseName("running_coach_codegen")
            .withUsername("running_coach")
            .withPassword("running_coach")) {
            postgres.start();
            migrate(postgres);
            generate(postgres, targetDirectory);
        }
    }

    /**
     * Aplica la misma historia Flyway que utiliza la aplicación contra el contenedor efímero.
     *
     * @param postgres contenedor PostgreSQL ya iniciado
     */
    private static void migrate(PostgreSQLContainer postgres) {
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .defaultSchema("platform")
            .schemas("platform")
            .createSchemas(true)
            .cleanDisabled(true)
            .locations("classpath:db/migration")
            .load()
            .migrate();
    }

    /**
     * Inspecciona los ocho esquemas técnicos ya migrados y escribe los tipos jOOQ fuera del
     * código fuente versionado.
     *
     * @param postgres contenedor PostgreSQL migrado
     * @param targetDirectory directorio de salida bajo {@code build/generated}
     * @throws Exception si jOOQ no puede generar las fuentes
     */
    private static void generate(PostgreSQLContainer postgres, Path targetDirectory) throws Exception {
        List<SchemaMappingType> schemaMappings = SCHEMAS.stream()
            .map(schema -> new SchemaMappingType().withInputSchema(schema).withOutputSchema(schema))
            .toList();

        Configuration configuration = new Configuration()
            .withJdbc(new Jdbc()
                .withDriver("org.postgresql.Driver")
                .withUrl(postgres.getJdbcUrl())
                .withUser(postgres.getUsername())
                .withPassword(postgres.getPassword()))
            .withGenerator(new Generator()
                .withName("org.jooq.codegen.JavaGenerator")
                .withDatabase(new Database()
                    .withName("org.jooq.meta.postgres.PostgresDatabase")
                    .withIncludes(".*")
                    .withExcludes("flyway_schema_history")
                    .withSchemata(schemaMappings))
                .withGenerate(new Generate()
                    .withDeprecated(false)
                    .withEmptySchemas(true)
                    .withGeneratedAnnotation(false)
                    .withJavaTimeTypes(true)
                    .withRecords(true)
                    .withPojos(false)
                    .withDaos(false))
                .withTarget(new Target()
                    .withPackageName("org.vgrunning.generated.jooq")
                    .withDirectory(targetDirectory.toString())
                    .withEncoding("UTF-8")
                    .withClean(true)));

        GenerationTool.generate(configuration);
    }
}
