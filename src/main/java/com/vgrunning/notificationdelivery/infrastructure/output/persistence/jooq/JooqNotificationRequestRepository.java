package com.vgrunning.notificationdelivery.infrastructure.output.persistence.jooq;

import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;
import com.vgrunning.notificationdelivery.application.port.out.NotificationRequestRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Guarda solicitudes autocontenidas de correo sin ejecutar llamadas externas en la transacción. */
@Repository
@RequiredArgsConstructor
public class JooqNotificationRequestRepository implements NotificationRequestRepository {
    private final DSLContext jooq;
    private final NotificationRequestPersistenceMapper mapper;

    @Override
    public void create(CreateNotificationRequest request) {
        var record = mapper.toRecord(request);
        record.setCreatedAt(
                jooq.select(DSL.currentOffsetDateTime()).fetchSingle(0, OffsetDateTime.class));
        jooq.executeInsert(record);
    }
}
