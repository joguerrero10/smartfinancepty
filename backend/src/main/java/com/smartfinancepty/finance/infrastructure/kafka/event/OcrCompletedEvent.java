package com.smartfinancepty.finance.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrCompletedEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long userId;
    private Long fileAttachmentId;
    private boolean processed;
    private BigDecimal detectedAmount;
    private String merchantName;
    private LocalDate invoiceDate;
    private double confidence;
    private Long createdExpenseId;

    public static final String TOPIC = "smartfinance.ocr.completed";
}
