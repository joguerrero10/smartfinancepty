package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private boolean isGlobal; // true = presupuesto global
    private BigDecimal limitAmount;
    private BigDecimal spentAmount; // lo que se ha gastado
    private BigDecimal remainingAmount;// lo que queda
    private double usagePercentage; // % usado
    private boolean isOverBudget; // superó el límite
    private boolean isNearLimit; // superó el 80%
    private String alertMessage; // mensaje de alerta
    private int year;
    private int month;
}
