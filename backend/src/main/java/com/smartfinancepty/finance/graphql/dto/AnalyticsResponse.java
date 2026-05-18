package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {
    private int year;
    private int month;
    private BigDecimal totalExpenses;
    private BigDecimal totalIncome;
    private double savingsRate;
    private List<CategoryAnalytic> topCategories;
    private List<TrendPoint> expenseTrend;
    private PredictionResult prediction;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String riskMessage;
}
