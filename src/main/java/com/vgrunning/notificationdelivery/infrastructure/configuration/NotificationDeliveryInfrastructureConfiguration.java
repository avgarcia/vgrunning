package com.vgrunning.notificationdelivery.infrastructure.configuration;

import com.vgrunning.notificationdelivery.api.request.NotificationRequestApi;
import com.vgrunning.notificationdelivery.application.port.out.NotificationRequestRepository;
import com.vgrunning.notificationdelivery.application.service.CreateNotificationRequestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Compone el caso de uso de creación de solicitudes de notificación. */
@Configuration(proxyBeanMethods = false)
public class NotificationDeliveryInfrastructureConfiguration {
    @Bean
    NotificationRequestApi notificationRequestApi(NotificationRequestRepository requests) {
        return new CreateNotificationRequestService(requests);
    }
}
