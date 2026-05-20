package com.smartfinancepty.finance.infrastructure.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancepty.finance.infrastructure.kafka.event.NotificationRequestedEvent;
import com.smartfinancepty.finance.infrastructure.kafka.event.OcrCompletedEvent;
import com.smartfinancepty.finance.infrastructure.kafka.event.OcrRequestedEvent;
import com.smartfinancepty.finance.infrastructure.kafka.producer.FinanceEventProducer;
import com.smartfinancepty.finance.service.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OcrEventConsumer {

    private final OcrService ocrService;
    private final FinanceEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    /**
     * Procesa solicitudes de OCR en background El usuario no espera — el resultado se notifica via
     * Kafka → Notification
     */
    @KafkaListener(
            topics = "#{T(com.smartfinancepty.finance.infrastructure.kafka.event.OcrRequestedEvent).TOPIC}",
            groupId = "${spring.kafka.consumer.group-id:smartfinance-group}-ocr",
            containerFactory = "kafkaListenerContainerFactory")
    public void onOcrRequested(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        OcrRequestedEvent event = null;
        try {
            log.info("📥 OCR requested event: partition={}, offset={}", record.partition(),
                    record.offset());

            event = objectMapper.convertValue(record.value(), OcrRequestedEvent.class);

            // Procesar OCR en background
            var result = ocrService.processFileAttachment(event.getFileAttachmentId(),
                    event.getUserId(), event.isAutoCreate(), event.getCategoryId());

            // Publicar resultado
            OcrCompletedEvent completed = OcrCompletedEvent.builder().userId(event.getUserId())
                    .fileAttachmentId(event.getFileAttachmentId()).processed(result.isProcessed())
                    .detectedAmount(result.getTotalAmount()).merchantName(result.getMerchantName())
                    .invoiceDate(result.getInvoiceDate()).confidence(result.getConfidence())
                    .build();

            eventProducer.publishOcrCompleted(completed);

            // Notificar al usuario el resultado
            if (result.isProcessed()) {
                NotificationRequestedEvent notification = NotificationRequestedEvent.builder()
                        .userId(event.getUserId()).title("✅ Factura procesada")
                        .message("Se detectó un gasto de $" + result.getTotalAmount() + " en "
                                + result.getMerchantName())
                        .notificationType("GENERAL").channel("PUSH")
                        .referenceId(event.getFileAttachmentId()).referenceType("FILE").build();

                eventProducer.publishNotificationRequested(notification);
            }

            ack.acknowledge();
            log.info("✅ OCR procesado: fileId={}, amount={}, confidence={}",
                    event.getFileAttachmentId(), result.getTotalAmount(), result.getConfidence());

        } catch (Exception e) {
            log.error("❌ Error procesando OCR: {}", e.getMessage(), e);
            if (event != null) {
                // Notificar fallo al usuario
                NotificationRequestedEvent failNotification = NotificationRequestedEvent.builder()
                        .userId(event.getUserId()).title("❌ Error al procesar factura")
                        .message("No se pudo leer la factura. Intenta con una imagen más clara.")
                        .notificationType("GENERAL").channel("PUSH").build();
                eventProducer.publishNotificationRequested(failNotification);
            }
            ack.acknowledge(); // ack para no bloquear la queue
        }
    }
}
