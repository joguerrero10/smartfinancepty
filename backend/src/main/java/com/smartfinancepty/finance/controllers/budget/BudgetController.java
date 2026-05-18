package com.smartfinancepty.finance.controllers.budget;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.BudgetRequest;
import com.smartfinancepty.finance.dto.BudgetResponse;
import com.smartfinancepty.finance.service.finance.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Gestión de presupuestos")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @Operation(summary = "Obtener presupuestos del mes actual")
    public ResponseEntity<List<BudgetResponse>> getCurrentMonthBudgets(
            @AuthenticationPrincipal User user) {
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(
                budgetService.getBudgetsByMonth(user.getId(), now.getYear(), now.getMonthValue()));
    }

    @GetMapping("/month")
    @Operation(summary = "Obtener presupuestos por mes")
    public ResponseEntity<List<BudgetResponse>> getBudgetsByMonth(@RequestParam int year,
            @RequestParam int month, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(budgetService.getBudgetsByMonth(user.getId(), year, month));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener presupuesto por ID")
    public ResponseEntity<BudgetResponse> getBudgetById(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(budgetService.getBudgetById(id, user.getId()));
    }

    @PostMapping
    @Operation(summary = "Crear presupuesto (global o por categoría)")
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.createBudget(request, user.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar presupuesto")
    public ResponseEntity<BudgetResponse> updateBudget(@PathVariable Long id,
            @Valid @RequestBody BudgetRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(budgetService.updateBudget(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar presupuesto (soft delete)")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        budgetService.deleteBudget(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
