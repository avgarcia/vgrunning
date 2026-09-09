package com.vgrunning.notificationdelivery.application.port.out;

import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;

/** Persiste una solicitud de notificación sin iniciar una llamada HTTP dentro de la transacción. */
public interface NotificationRequestRepository {
    void create(CreateNotificationRequest request);
}
