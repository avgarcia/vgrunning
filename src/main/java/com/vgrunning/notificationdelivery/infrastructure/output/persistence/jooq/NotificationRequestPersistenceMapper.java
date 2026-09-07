package com.vgrunning.notificationdelivery.infrastructure.output.persistence.jooq;

import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.vgrunning.generated.jooq.notification_delivery.tables.records.NotificationRequestRecord;

/** Traduce la solicitud publicada al único registro de outbox que necesita F01.2. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationRequestPersistenceMapper {
    @Mapping(target = "originType", constant = "invitation")
    @Mapping(target = "status", constant = "pending")
    @Mapping(target = "destinationKeyId", expression = "java(request.destination().getKeyId())")
    @Mapping(target = "destinationNonce", expression = "java(request.destination().getNonce())")
    @Mapping(
            target = "destinationCiphertext",
            expression = "java(request.destination().getCiphertext())")
    @Mapping(target = "payloadKeyId", expression = "java(request.payload().getKeyId())")
    @Mapping(target = "payloadNonce", expression = "java(request.payload().getNonce())")
    @Mapping(target = "payloadCiphertext", expression = "java(request.payload().getCiphertext())")
    @Mapping(target = "createdAt", ignore = true)
    NotificationRequestRecord toRecord(CreateNotificationRequest request);
}
