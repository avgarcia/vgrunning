package com.vgrunning.identityaccess.adapter.out.persistence.jooq;

import static org.vgrunning.generated.jooq.identity_access.tables.SecurityEvent.SECURITY_EVENT;

import com.vgrunning.identityaccess.application.port.out.SecurityEventRepository;
import com.vgrunning.identityaccess.application.securityevent.SecurityEvent;
import com.vgrunning.identityaccess.infrastructure.configuration.IdentityAccessProperties;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

/** Adaptador append-only de auditoría de acceso. */
@Repository
@RequiredArgsConstructor
public class JooqSecurityEventRepository implements SecurityEventRepository {
    private final DSLContext jooq;
    private final IdentityAccessProperties properties;

    @Override
    public void append(SecurityEvent event) {
        jooq.insertInto(SECURITY_EVENT)
                .set(SECURITY_EVENT.ID, event.id())
                .set(SECURITY_EVENT.OCCURRED_AT, event.occurredAt())
                .set(
                        SECURITY_EVENT.RETENTION_UNTIL,
                        event.occurredAt().plus(properties.securityEventRetention()))
                .set(SECURITY_EVENT.EVENT_TYPE, event.type().value())
                .set(SECURITY_EVENT.OUTCOME, event.outcome().value())
                .set(SECURITY_EVENT.ACTOR_CLASS, event.actorClass().value())
                .set(SECURITY_EVENT.ACTOR_ACCOUNT_ID, event.actorAccountId())
                .set(SECURITY_EVENT.AFFECTED_ACCOUNT_ID, event.affectedAccountId())
                .set(SECURITY_EVENT.ACCESS_SESSION_ID, event.sessionId())
                .set(SECURITY_EVENT.CORRELATION_ID, event.correlationId())
                .set(SECURITY_EVENT.METADATA, JSONB.valueOf("{}"))
                .execute();
    }
}
