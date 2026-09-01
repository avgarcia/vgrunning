package com.vgrunning.identityaccess.adapter.out.persistence.jooq;

import com.vgrunning.identityaccess.application.port.out.CurrentTimeProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Obtiene el instante autoritativo del PostgreSQL que participa en la transacción actual. */
@Component
public final class JooqCurrentTimeProvider implements CurrentTimeProvider {
    private final DSLContext jooq;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "DSLContext es una dependencia inyectada y gestionada por Spring/jOOQ.")
    public JooqCurrentTimeProvider(DSLContext jooq) {
        this.jooq = jooq;
    }

    @Override
    public OffsetDateTime now() {
        Field<OffsetDateTime> currentTime = DSL.currentOffsetDateTime();
        return Objects.requireNonNull(jooq.select(currentTime).fetchOne(currentTime));
    }
}
