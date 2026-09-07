package com.vgrunning.runnermanagement.infrastructure.output.persistence.jooq;

import static org.vgrunning.generated.jooq.runner_management.tables.Runner.RUNNER;
import static org.vgrunning.generated.jooq.runner_management.tables.RunnerLifecycleAudit.RUNNER_LIFECYCLE_AUDIT;

import com.vgrunning.runnermanagement.application.port.out.RunnerActivationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Actualiza idempotentemente el perfil después de una activación duradera de identidad. */
@Repository
@RequiredArgsConstructor
public class JooqRunnerActivationRepository implements RunnerActivationRepository {
    private final DSLContext jooq;

    @Override
    public void activate(UUID accountId, UUID correlationId) {
        jooq.update(RUNNER)
                .set(RUNNER.STATUS, "active")
                .set(RUNNER.ACTIVATED_AT, DSL.currentOffsetDateTime())
                .set(RUNNER.VERSION, RUNNER.VERSION.plus(1L))
                .where(RUNNER.ACCOUNT_ID.eq(accountId))
                .and(RUNNER.STATUS.eq("pending_activation"))
                .returning(RUNNER.ID)
                .fetchOptional()
                .ifPresent(
                        runner ->
                                jooq.insertInto(RUNNER_LIFECYCLE_AUDIT)
                                        .set(RUNNER_LIFECYCLE_AUDIT.ID, UUID.randomUUID())
                                        .set(
                                                RUNNER_LIFECYCLE_AUDIT.RUNNER_ID,
                                                runner.get(RUNNER.ID))
                                        .set(RUNNER_LIFECYCLE_AUDIT.ACTOR_KIND, "system")
                                        .set(RUNNER_LIFECYCLE_AUDIT.TRANSITION, "activated")
                                        .set(
                                                RUNNER_LIFECYCLE_AUDIT.OCCURRED_AT,
                                                DSL.currentOffsetDateTime())
                                        .set(RUNNER_LIFECYCLE_AUDIT.CORRELATION_ID, correlationId)
                                        .execute());
    }
}
