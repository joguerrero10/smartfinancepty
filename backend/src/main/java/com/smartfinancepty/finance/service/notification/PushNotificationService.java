package com.smartfinancepty.finance.service.notification;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PushNotificationService {

    /**
     * Envía una push notification. En producción integrar con Firebase FCM. Por ahora loggea el
     * intento para desarrollo.
     */
    public void sendPush(String userId, String title, String body) {
        log.info("🔔 PUSH → UserId: {} | Title: {} | Body: {}", userId, title, body);
        // TODO Fase 4: integrar con Firebase FCM
        // FirebaseMessaging.getInstance().send(
        // Message.builder()
        // .setNotification(Notification.builder().setTitle(title).setBody(body).build())
        // .setToken(deviceToken)
        // .build()
        // );
    }
}
