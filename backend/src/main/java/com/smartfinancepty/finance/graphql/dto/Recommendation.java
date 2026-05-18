package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {
    private String id;
    private String type;
    private String title;
    private String message;
    private String priority;
    private BigDecimal potentialSavings;
    private String categoryName;
    private boolean actionable;
}
