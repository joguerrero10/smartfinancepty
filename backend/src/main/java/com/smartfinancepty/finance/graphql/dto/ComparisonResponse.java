package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonResponse {
    private MonthSummary currentMonth;
    private MonthSummary previousMonth;
    private BigDecimal expenseDifference;
    private double expenseChangePercentage;
    private boolean increased;
    private List<CategoryChange> categoriesIncreased;
    private List<CategoryChange> categoriesDecreased;
}
