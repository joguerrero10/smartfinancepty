package com.smartfinancepty.finance.controllers.expenses;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smartfinancepty.finance.domain.ExpenseType;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.ExpenseRequest;
import com.smartfinancepty.finance.dto.ExpenseResponse;
import com.smartfinancepty.finance.service.finance.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Gestión de gastos")
@SecurityRequirement(name = "bearerAuth")
public class ExpensesController {

    private final ExpenseService expenseService;

    @GetMapping
    @Operation(summary = "Obtener todos los gastos del usuario")
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expenseService.getAllExpenses(user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener gasto por ID")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expenseService.getExpenseById(id, user.getId()));
    }

    @GetMapping("/range")
    @Operation(summary = "Obtener gastos por rango de fechas")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expenseService.getExpensesByDateRange(user.getId(), start, end));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Obtener gastos por tipo (FIXED/VARIABLE)")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByType(@PathVariable ExpenseType type,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expenseService.getExpensesByType(user.getId(), type));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo gasto")
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createExpense(request, user.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar gasto")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar gasto (soft delete)")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        expenseService.deleteExpense(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
