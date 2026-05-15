package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.smartfinancepty.finance.domain.FrequencyType;
import com.smartfinancepty.finance.domain.IncomeType;
import lombok.*;

// ============================================================
// IncomeRequest.java
// ============================================================
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeRequest {

    @NotBlank(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotNull(message = "La frecuencia es requerida")
    private FrequencyType frequency;

    @NotNull(message = "El tipo de ingreso es requerido")
    private IncomeType incomeType;

    private List<DeductionRequest> deductions;
}
