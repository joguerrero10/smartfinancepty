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
public class ExpenseCreatedEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long userId;
    private Long expenseId;
    private BigDecimal amount;
    private String categoryName;
    private Long categoryId;
    private String description;
    private LocalDate expenseDate;
    private String expenseType;

    public static final String TOPIC = "smartfinance.expense.created";
}
