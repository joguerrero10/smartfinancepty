package com.smartfinancepty.finance.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDeletedEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long userId;
    private Long expenseId;
    private BigDecimal amount;
    private Long categoryId;

    public static final String TOPIC = "smartfinance.expense.deleted";
}
