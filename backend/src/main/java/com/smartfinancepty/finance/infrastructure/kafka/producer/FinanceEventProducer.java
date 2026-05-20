package com.smartfinancepty.finance.infrastructure.kafka.producer;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import com.smartfinancepty.finance.infrastructure.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Expense Events ────────────────────────────────────────────────────────────

    public void publishExpenseCreated(ExpenseCreatedEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("EXPENSE_CREATED");
        event.setOccurredAt(LocalDateTime.now());
        publish(ExpenseCreatedEvent.TOPIC, event.getUserId().toString(), event);
    }

    public void publishExpenseDeleted(ExpenseDeletedEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("EXPENSE_DELETED");
        event.setOccurredAt(LocalDateTime.now());
        publish(ExpenseDeletedEvent.TOPIC, event.getUserId().toString(), event);
    }

    // ── Budget Events ─────────────────────────────────────────────────────────────

    public void publishBudgetAlert(BudgetAlertEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("BUDGET_ALERT");
        event.setOccurredAt(LocalDateTime.now());
        publish(BudgetAlertEvent.TOPIC, event.getUserId().toString(), event);
    }

    // ── OCR Events ────────────────────────────────────────────────────────────────

    public void publishOcrRequested(OcrRequestedEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("OCR_REQUESTED");
        event.setOccurredAt(LocalDateTime.now());
        publish(OcrRequestedEvent.TOPIC, event.getFileAttachmentId().toString(), event);
    }

    public void publishOcrCompleted(OcrCompletedEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("OCR_COMPLETED");
        event.setOccurredAt(LocalDateTime.now());
        publish(OcrCompletedEvent.TOPIC, event.getFileAttachmentId().toString(), event);
    }

    // ── Notification Events ───────────────────────────────────────────────────────

    public void publishNotificationRequested(NotificationRequestedEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("NOTIFICATION_REQUESTED");
        event.setOccurredAt(LocalDateTime.now());
        publish(NotificationRequestedEvent.TOPIC, event.getUserId().toString(), event);
    }

    // ── Core publish ──────────────────────────────────────────────────────────

    private void publish(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("❌ Kafka send failed → topic={}, key={}, error={}", topic, key,
                        ex.getMessage());
            } else {
                log.debug("✅ Kafka sent → topic={}, partition={}, offset={}", topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
