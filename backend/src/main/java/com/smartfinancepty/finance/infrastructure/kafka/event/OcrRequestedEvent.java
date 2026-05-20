package com.smartfinancepty.finance.infrastructure.kafka.event;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrRequestedEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long userId;
    private Long fileAttachmentId;
    private String filePath;
    private String contentType;
    private boolean autoCreate;
    private Long categoryId;

    public static final String TOPIC = "smartfinance.ocr.requested";
}
