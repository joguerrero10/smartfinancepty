package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import com.smartfinancepty.finance.domain.DeductionType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeductionResponse {
    private Long id;
    private String name;
    private DeductionType deductionType;
    private boolean isPercentage;
    private BigDecimal value;
    private BigDecimal calculatedAmount; // monto calculado sobre el ingreso
}
