package com.smartfinancepty.finance.controllers.income;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.IncomeRequest;
import com.smartfinancepty.finance.dto.IncomeResponse;
import com.smartfinancepty.finance.service.finance.IncomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/incomes")
@RequiredArgsConstructor
@Tag(name = "Incomes", description = "Gestión de ingresos")
@SecurityRequirement(name = "bearerAuth")
public class IncomeController {

    private final IncomeService incomeService;

    @GetMapping
    @Operation(summary = "Obtener todos los ingresos del usuario")
    public ResponseEntity<List<IncomeResponse>> getAllIncomes(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(incomeService.getAllIncomes(user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ingreso por ID")
    public ResponseEntity<IncomeResponse> getIncomeById(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(incomeService.getIncomeById(id, user.getId()));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo ingreso")
    public ResponseEntity<IncomeResponse> createIncome(@Valid @RequestBody IncomeRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeService.createIncome(request, user.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar ingreso")
    public ResponseEntity<IncomeResponse> updateIncome(@PathVariable Long id,
            @Valid @RequestBody IncomeRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(incomeService.updateIncome(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar ingreso (soft delete)")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        incomeService.deleteIncome(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
