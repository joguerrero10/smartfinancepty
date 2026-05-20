package com.smartfinancepty.finance.infrastructure.kafka.event;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestedEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long userId;
    private String title;
    private String message;
    private String notificationType;
    private String channel;
    private Long referenceId;
    private String referenceType;

    public static final String TOPIC = "smartfinance.notification.requested";
}
