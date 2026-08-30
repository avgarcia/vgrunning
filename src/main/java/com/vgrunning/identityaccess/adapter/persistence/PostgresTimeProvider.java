package com.vgrunning.identityaccess.adapter.persistence;

import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Fuente de tiempo autoritativa de las transacciones críticas de identidad. */
@Component
public class PostgresTimeProvider {
    private final JdbcTemplate jdbc;

    public PostgresTimeProvider(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public OffsetDateTime now() {
        return Objects.requireNonNull(
                jdbc.queryForObject("SELECT CURRENT_TIMESTAMP", OffsetDateTime.class),
                "PostgreSQL debe devolver el instante actual.");
    }
}
