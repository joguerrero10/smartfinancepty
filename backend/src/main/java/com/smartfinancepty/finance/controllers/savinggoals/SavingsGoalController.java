package com.smartfinancepty.finance.controllers.savinggoals;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.SavingsGoalRequest;
import com.smartfinancepty.finance.dto.SavingsGoalResponse;
import com.smartfinancepty.finance.service.finance.SavingsGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/savings-goals")
@RequiredArgsConstructor
@Tag(name = "Savings Goals", description = "Gestión de metas de ahorro")
@SecurityRequirement(name = "bearerAuth")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    @GetMapping
    @Operation(summary = "Obtener todas las metas de ahorro del mes actual")
    public ResponseEntity<List<SavingsGoalResponse>> getAllGoals(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(savingsGoalService.getAllGoals(user.getId()));
    }

    @GetMapping("/month")
    @Operation(summary = "Obtener metas por mes específico")
    public ResponseEntity<List<SavingsGoalResponse>> getGoalsByMonth(@RequestParam int year,
            @RequestParam int month, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(savingsGoalService.getGoalsByMonth(user.getId(), year, month));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener meta por ID")
    public ResponseEntity<SavingsGoalResponse> getGoalById(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(savingsGoalService.getGoalById(id, user.getId()));
    }

    @PostMapping
    @Operation(summary = "Crear meta de ahorro")
    public ResponseEntity<SavingsGoalResponse> createGoal(
            @Valid @RequestBody SavingsGoalRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingsGoalService.createGoal(request, user.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar meta de ahorro")
    public ResponseEntity<SavingsGoalResponse> updateGoal(@PathVariable Long id,
            @Valid @RequestBody SavingsGoalRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(savingsGoalService.updateGoal(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar meta de ahorro (soft delete)")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        savingsGoalService.deleteGoal(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
