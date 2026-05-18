package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryAnalytic {
    private String categoryName;
    private BigDecimal totalAmount;
    private double percentage;
    private String trend;
    private double trendPercentage;
    private int expenseCount;
}
