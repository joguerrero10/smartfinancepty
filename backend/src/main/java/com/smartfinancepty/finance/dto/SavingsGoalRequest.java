package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

// ── SavingsGoalRequest ────────────────────────────────────────────────────────
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalRequest {

    @NotBlank(message = "El nombre es requerido")
    private String name;

    // Al menos uno de los dos debe estar presente
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal fixedAmount;

    @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede superar 100")
    private BigDecimal percentage;
}
