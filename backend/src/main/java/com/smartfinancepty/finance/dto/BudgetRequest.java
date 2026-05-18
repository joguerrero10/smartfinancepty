package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

// ── BudgetRequest ─────────────────────────────────────────────────────────────
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRequest {
    // null = presupuesto global
    private Long categoryId;

    @NotNull(message = "El monto límite es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal limitAmount;

    @NotNull(message = "El año es requerido")
    @Min(value = 2000, message = "Año inválido")
    @Max(value = 2100, message = "Año inválido")
    private Integer year;

    @NotNull(message = "El mes es requerido")
    @Min(value = 1, message = "El mes debe estar entre 1 y 12")
    @Max(value = 12, message = "El mes debe estar entre 1 y 12")
    private Integer month;

}
