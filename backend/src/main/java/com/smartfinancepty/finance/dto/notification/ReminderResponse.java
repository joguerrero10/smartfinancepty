package com.smartfinancepty.finance.dto.notification;

import java.time.LocalDateTime;
import com.smartfinancepty.finance.domain.notification.NotificationChannel;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationChannel channel;
    private Integer dayOfMonth;
    private LocalDateTime remindAt;
    private boolean recurring;
    private boolean active;
    private Long expenseId;
    private String expenseDescription;
    private LocalDateTime createdAt;
}
