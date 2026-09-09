package com.vgrunning.notificationdelivery.application.service;

import com.vgrunning.notificationdelivery.api.request.CreateNotificationRequest;
import com.vgrunning.notificationdelivery.api.request.NotificationRequestApi;
import com.vgrunning.notificationdelivery.application.port.out.NotificationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordina la creación de una solicitud de notificación sin exponer persistencia a otros módulos.
 */
@RequiredArgsConstructor
public class CreateNotificationRequestService implements NotificationRequestApi {
    private final NotificationRequestRepository requests;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void create(CreateNotificationRequest request) {
        requests.create(request);
    }
}
