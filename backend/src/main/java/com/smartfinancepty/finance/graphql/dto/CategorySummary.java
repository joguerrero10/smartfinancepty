package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySummary {
    private Long categoryId;
    private String categoryName;
    private BigDecimal totalAmount;
    private double percentage;
    private int expenseCount;
    private String color;
    private String icon;

}
