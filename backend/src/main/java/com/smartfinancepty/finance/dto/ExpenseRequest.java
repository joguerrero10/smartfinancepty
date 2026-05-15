package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.smartfinancepty.finance.domain.ExpenseType;
import com.smartfinancepty.finance.domain.FrequencyType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequest {

    @NotBlank(message = "La descripción es requerida")
    private String description;

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotNull(message = "La categoría es requerida")
    private Long categoryId;

    private Long subCategoryId;

    @NotNull(message = "El tipo de gasto es requerido")
    private ExpenseType expenseType;

    @NotNull(message = "La fecha es requerida")
    private LocalDate expenseDate;

    private FrequencyType frequency; // solo para gastos fijos
    private Integer dueDay; // día de vencimiento
    private String notes;
}
