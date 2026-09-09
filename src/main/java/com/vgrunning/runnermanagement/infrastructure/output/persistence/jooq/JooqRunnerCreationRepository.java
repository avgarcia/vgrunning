package com.vgrunning.runnermanagement.infrastructure.output.persistence.jooq;

import static org.vgrunning.generated.jooq.runner_management.tables.Runner.RUNNER;
import static org.vgrunning.generated.jooq.runner_management.tables.RunnerCreationIdempotency.RUNNER_CREATION_IDEMPOTENCY;
import static org.vgrunning.generated.jooq.runner_management.tables.RunnerLifecycleAudit.RUNNER_LIFECYCLE_AUDIT;

import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository;
import com.vgrunning.runnermanagement.domain.Runner;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Persistencia jOOQ del alta atómica de perfiles y de su idempotencia. */
@Repository
@RequiredArgsConstructor
public class JooqRunnerCreationRepository implements RunnerCreationRepository {
    private final DSLContext jooq;
    private final RunnerPersistenceMapper mapper;

    @Override
    public Optional<StoredCreation> reserve(
            UUID administratorId, UUID idempotencyKey, byte[] fingerprint) {
        jooq.deleteFrom(RUNNER_CREATION_IDEMPOTENCY)
                .where(RUNNER_CREATION_IDEMPOTENCY.ADMINISTRATOR_ACCOUNT_ID.eq(administratorId))
                .and(RUNNER_CREATION_IDEMPOTENCY.IDEMPOTENCY_KEY.eq(idempotencyKey))
                .and(RUNNER_CREATION_IDEMPOTENCY.EXPIRES_AT.le(DSL.currentOffsetDateTime()))
                .execute();
        boolean created =
                jooq.insertInto(RUNNER_CREATION_IDEMPOTENCY)
                                .set(
                                        RUNNER_CREATION_IDEMPOTENCY.ADMINISTRATOR_ACCOUNT_ID,
                                        administratorId)
                                .set(RUNNER_CREATION_IDEMPOTENCY.IDEMPOTENCY_KEY, idempotencyKey)
                                .set(RUNNER_CREATION_IDEMPOTENCY.REQUEST_FINGERPRINT, fingerprint)
                                .set(
                                        RUNNER_CREATION_IDEMPOTENCY.CREATED_AT,
                                        DSL.currentOffsetDateTime())
                                .set(
                                        RUNNER_CREATION_IDEMPOTENCY.EXPIRES_AT,
                                        DSL.field(
                                                "CURRENT_TIMESTAMP + INTERVAL '24 hours'",
                                                OffsetDateTime.class))
                                .onConflictDoNothing()
                                .execute()
                        == 1;
        return created ? Optional.empty() : find(administratorId, idempotencyKey);
    }

    @Override
    public Runner create(NewRunner runner) {
        var record = mapper.toRunnerRecord(runner);
        record.setCreatedAt(
                jooq.select(DSL.currentOffsetDateTime()).fetchSingle(0, OffsetDateTime.class));
        jooq.executeInsert(record);
        jooq.insertInto(RUNNER_LIFECYCLE_AUDIT)
                .set(RUNNER_LIFECYCLE_AUDIT.ID, UUID.randomUUID())
                .set(RUNNER_LIFECYCLE_AUDIT.RUNNER_ID, runner.runnerId())
                .set(RUNNER_LIFECYCLE_AUDIT.ACTOR_KIND, "administrator")
                .set(RUNNER_LIFECYCLE_AUDIT.ACTOR_ACCOUNT_ID, runner.actorId())
                .set(RUNNER_LIFECYCLE_AUDIT.TRANSITION, "created")
                .set(RUNNER_LIFECYCLE_AUDIT.OCCURRED_AT, DSL.currentOffsetDateTime())
                .set(RUNNER_LIFECYCLE_AUDIT.CORRELATION_ID, runner.correlationId())
                .execute();
        return mapper.toDomain(record);
    }

    @Override
    public void complete(UUID administratorId, UUID idempotencyKey, Runner runner) {
        jooq.update(RUNNER_CREATION_IDEMPOTENCY)
                .set(RUNNER_CREATION_IDEMPOTENCY.RUNNER_ID, runner.id())
                .where(RUNNER_CREATION_IDEMPOTENCY.ADMINISTRATOR_ACCOUNT_ID.eq(administratorId))
                .and(RUNNER_CREATION_IDEMPOTENCY.IDEMPOTENCY_KEY.eq(idempotencyKey))
                .execute();
    }

    private Optional<StoredCreation> find(UUID administratorId, UUID idempotencyKey) {
        return jooq.selectFrom(RUNNER_CREATION_IDEMPOTENCY)
                .where(RUNNER_CREATION_IDEMPOTENCY.ADMINISTRATOR_ACCOUNT_ID.eq(administratorId))
                .and(RUNNER_CREATION_IDEMPOTENCY.IDEMPOTENCY_KEY.eq(idempotencyKey))
                .fetchOptional()
                .flatMap(
                        reservation ->
                                jooq.selectFrom(RUNNER)
                                        .where(RUNNER.ID.eq(reservation.getRunnerId()))
                                        .fetchOptional(
                                                runner ->
                                                        mapper.toStoredCreation(
                                                                reservation.getRequestFingerprint(),
                                                                runner)));
    }
}
