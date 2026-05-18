package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResult {
    private BigDecimal predictedExpenses;
    private BigDecimal predictedSavings;
    private String confidenceLevel;
    private int basedOnMonths;
}
