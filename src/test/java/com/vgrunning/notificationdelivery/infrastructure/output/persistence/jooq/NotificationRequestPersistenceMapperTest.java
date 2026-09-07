package com.vgrunning.notificationdelivery.infrastructure.output.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;
import com.vgrunning.notificationdelivery.api.request.EncryptedValue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationRequestPersistenceMapperTest {
    private final NotificationRequestPersistenceMapper mapper =
            new NotificationRequestPersistenceMapperImpl();

    @Test
    void mapsOnlyEncryptedNotificationPayloads() {
        CreateNotificationRequest request =
                new CreateNotificationRequest(
                        UUID.randomUUID(),
                        "invitation:123",
                        UUID.randomUUID(),
                        new EncryptedValue("key-1", new byte[] {1}, new byte[] {2}),
                        new EncryptedValue("key-1", new byte[] {3}, new byte[] {4}),
                        UUID.randomUUID());

        assertThat(mapper.toRecord(request))
                .extracting(
                        "logicalKey", "originType", "status", "destinationKeyId", "payloadKeyId")
                .containsExactly("invitation:123", "invitation", "pending", "key-1", "key-1");
        assertThat(mapper.toRecord(null)).isNull();
    }
}
