package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryChange {
    private String categoryName;
    private BigDecimal previousAmount;
    private BigDecimal currentAmount;
    private double changePercentage;
    private BigDecimal changeAmount;
}
