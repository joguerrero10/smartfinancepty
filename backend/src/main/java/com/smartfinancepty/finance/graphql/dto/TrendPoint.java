package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendPoint {
    private String label;
    private BigDecimal amount;
    private int month;
    private int year;
}
