package com.vgrunning.runnerportal.adapter.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

class SpaResourceResolverTest {
    @TempDir private Path staticDirectory;

    private SpaResourceResolver resolver;
    private Resource location;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(staticDirectory.resolve("index.html"), "index");
        Files.writeString(staticDirectory.resolve("application.js"), "script");
        resolver = new SpaResourceResolver();
        location = new FileSystemResource(staticDirectory + File.separator);
    }

    @Test
    void resolvesTheIndexForTheRootAndMissingClientRoutes() throws IOException {
        assertThat(Objects.requireNonNull(resolver.getResource("", location)).getFilename())
                .isEqualTo("index.html");
        assertThat(Objects.requireNonNull(resolver.getResource("mi-plan", location)).getFilename())
                .isEqualTo("index.html");
        assertThat(
                        Objects.requireNonNull(resolver.getResource("/historial/semana", location))
                                .getFilename())
                .isEqualTo("index.html");
    }

    @Test
    void resolvesAnExistingStaticResourceBeforeApplyingFallback() throws IOException {
        assertThat(
                        Objects.requireNonNull(resolver.getResource("application.js", location))
                                .getFilename())
                .isEqualTo("application.js");
    }

    @ParameterizedTest
    @MethodSource("technicalAndStaticPaths")
    void neverFallsBackForReservedPaths(String path) throws IOException {
        assertThat(resolver.getResource(path, location)).isNull();
    }

    @Test
    void neverFallsBackForAMissingFileLikePath() throws IOException {
        assertThat(resolver.getResource("icons/missing.svg", location)).isNull();
    }

    private static Stream<Arguments> technicalAndStaticPaths() {
        return Stream.of(
                Arguments.of("api"),
                Arguments.of("api/sessions"),
                Arguments.of("actuator"),
                Arguments.of("actuator/health"),
                Arguments.of("assets"),
                Arguments.of("assets/missing"),
                Arguments.of("error"),
                Arguments.of("error/details"),
                Arguments.of("webjars"),
                Arguments.of("webjars/library"));
    }
}
