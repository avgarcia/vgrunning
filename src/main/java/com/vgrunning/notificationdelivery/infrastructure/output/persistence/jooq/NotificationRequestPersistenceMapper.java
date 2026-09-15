package com.vgrunning.notificationdelivery.infrastructure.output.persistence.jooq;

import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;
import org.springframework.stereotype.Component;
import org.vgrunning.generated.jooq.notification_delivery.tables.records.NotificationRequestRecord;

/** Traduce la solicitud publicada al único registro de outbox que necesita F01.2. */
@Component
public class NotificationRequestPersistenceMapper {
    public NotificationRequestRecord toRecord(CreateNotificationRequest request) {
        if (request == null) {
            return null;
        }
        NotificationRequestRecord record = new NotificationRequestRecord();
        record.setId(request.id());
        record.setLogicalKey(request.logicalKey());
        record.setOriginType("invitation");
        record.setOriginId(request.originId());
        record.setStatus("pending");
        record.setDestinationKeyId(request.destination().getKeyId());
        record.setDestinationNonce(request.destination().getNonce());
        record.setDestinationCiphertext(request.destination().getCiphertext());
        record.setPayloadKeyId(request.payload().getKeyId());
        record.setPayloadNonce(request.payload().getNonce());
        record.setPayloadCiphertext(request.payload().getCiphertext());
        record.setCorrelationId(request.correlationId());
        return record;
    }
}
