package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.smartfinancepty.finance.domain.ExpenseType;
import com.smartfinancepty.finance.domain.FrequencyType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    private Long id;
    private String description;
    private BigDecimal amount;
    private ExpenseType expenseType;
    private String categoryName;
    private String subCategoryName;
    private LocalDate expenseDate;
    private FrequencyType frequency;
    private Integer dueDay;
    private String notes;
    private boolean active;
    private LocalDateTime createdAt;
}
