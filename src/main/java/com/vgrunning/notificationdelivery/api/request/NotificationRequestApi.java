package com.vgrunning.notificationdelivery.api.request;

/** Persiste una solicitud de notificación sin iniciar una llamada HTTP dentro de la transacción. */
public interface NotificationRequestApi {
    void create(CreateNotificationRequest command);
}
