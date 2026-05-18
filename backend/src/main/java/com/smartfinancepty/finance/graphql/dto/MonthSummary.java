package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthSummary {
    private int year;
    private int month;
    private BigDecimal totalExpenses;
    private BigDecimal totalIncome;
    private BigDecimal balance;
}
