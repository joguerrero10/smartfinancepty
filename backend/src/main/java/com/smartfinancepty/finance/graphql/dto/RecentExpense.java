package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentExpense {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String categoryName;
    private String subCategoryName;
    private String expenseType;
    private LocalDate expenseDate;

}
