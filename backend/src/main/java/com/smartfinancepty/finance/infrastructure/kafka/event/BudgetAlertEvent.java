package com.smartfinancepty.finance.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAlertEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long userId;
    private Long budgetId;
    private String categoryName;
    private boolean isGlobal;
    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private double usagePercentage;
    private boolean exceeded;

    public static final String TOPIC = "smartfinance.budget.alert";
}
