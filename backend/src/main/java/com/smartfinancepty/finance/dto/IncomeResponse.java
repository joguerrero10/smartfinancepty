package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.smartfinancepty.finance.domain.FrequencyType;
import com.smartfinancepty.finance.domain.IncomeType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeResponse {
    private Long id;
    private String name;
    private BigDecimal amount;
    private BigDecimal netAmount; // amount - deducciones
    private BigDecimal totalDeductions; // suma de deducciones
    private FrequencyType frequency;
    private IncomeType incomeType;
    private boolean active;
    private List<DeductionResponse> deductions;
    private LocalDateTime createdAt;
}
