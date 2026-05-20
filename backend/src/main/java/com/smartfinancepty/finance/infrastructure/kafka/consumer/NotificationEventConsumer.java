package com.smartfinancepty.finance.infrastructure.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.domain.notification.NotificationChannel;
import com.smartfinancepty.finance.domain.notification.NotificationType;
import com.smartfinancepty.finance.infrastructure.kafka.event.NotificationRequestedEvent;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Procesa requests de notificación de forma asíncrona
     */
    @KafkaListener(
            topics = "#{T(com.smartfinancepty.finance.infrastructure.kafka.event.NotificationRequestedEvent).TOPIC}",
            groupId = "${spring.kafka.consumer.group-id:smartfinance-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onNotificationRequested(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            log.info("📥 Notification event received: partition={}, offset={}", record.partition(),
                    record.offset());

            NotificationRequestedEvent event =
                    objectMapper.convertValue(record.value(), NotificationRequestedEvent.class);

            User user = userRepository.findById(event.getUserId()).orElse(null);

            if (user == null) {
                log.warn("Usuario no encontrado para notificación: {}", event.getUserId());
                ack.acknowledge();
                return;
            }

            notificationService.sendNotification(user, event.getTitle(), event.getMessage(),
                    NotificationType.valueOf(event.getNotificationType()),
                    NotificationChannel.valueOf(event.getChannel()), event.getReferenceId(),
                    event.getReferenceType());

            ack.acknowledge();
            log.info("✅ Notificación enviada: userId={}, type={}, channel={}", event.getUserId(),
                    event.getNotificationType(), event.getChannel());

        } catch (Exception e) {
            log.error("❌ Error procesando notificación: {}", e.getMessage(), e);
        }
    }
}
