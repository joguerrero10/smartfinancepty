package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeSummary {
    private Long id;
    private String name;
    private String incomeType;
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private BigDecimal totalDeductions;
    private String frequency;

}
