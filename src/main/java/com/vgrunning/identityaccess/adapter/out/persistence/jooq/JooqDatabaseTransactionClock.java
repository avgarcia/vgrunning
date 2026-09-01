package com.vgrunning.identityaccess.adapter.out.persistence.jooq;

import com.vgrunning.identityaccess.application.port.out.DatabaseTransactionClock;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Lee el reloj autoritativo de PostgreSQL dentro de la transacción en curso. */
@Component
@RequiredArgsConstructor
public final class JooqDatabaseTransactionClock implements DatabaseTransactionClock {
    private final DSLContext jooq;

    @Override
    public OffsetDateTime now() {
        Field<OffsetDateTime> currentTime = DSL.currentOffsetDateTime();
        return Objects.requireNonNull(jooq.select(currentTime).fetchOne(currentTime));
    }
}
