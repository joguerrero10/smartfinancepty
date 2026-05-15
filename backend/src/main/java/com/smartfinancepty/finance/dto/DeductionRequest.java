package com.smartfinancepty.finance.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.smartfinancepty.finance.domain.DeductionType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeductionRequest {

    @NotBlank(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "El tipo de deducción es requerido")
    private DeductionType deductionType;

    @NotNull
    private boolean isPercentage;

    @NotNull(message = "El valor es requerido")
    @DecimalMin(value = "0.01", message = "El valor debe ser mayor a 0")
    private BigDecimal value;
}
