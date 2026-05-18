package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalResponse {
    private Long id;
    private String name;
    private BigDecimal fixedAmount;
    private BigDecimal percentage;
    private BigDecimal targetAmount; // monto objetivo calculado
    private BigDecimal actualSavings; // ahorro real del mes
    private boolean isAchieved; // ¿se cumplió la meta?
    private double achievementPercentage; // % de la meta cumplida
    private String statusMessage; // mensaje de estado
}
