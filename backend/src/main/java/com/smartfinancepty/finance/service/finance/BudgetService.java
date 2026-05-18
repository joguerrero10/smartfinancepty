package com.smartfinancepty.finance.service.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartfinancepty.finance.domain.Budget;
import com.smartfinancepty.finance.domain.ExpenseCategory;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.BudgetRequest;
import com.smartfinancepty.finance.dto.BudgetResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.BudgetRepository;
import com.smartfinancepty.finance.repository.ExpenseCategoryRepository;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final double ALERT_THRESHOLD = 80.0;

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByMonth(Long userId, int year, int month) {
        return budgetRepository.findByUserIdAndPeriodWithCategory(userId, year, month).stream()
                .map(b -> toResponse(b, year, month)).toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Presupuesto no encontrado"));
        return toResponse(budget, budget.getYear(), budget.getMonth());
    }

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        ExpenseCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

            // Verificar que no exista ya un presupuesto para esa categoría/mes
            budgetRepository
                    .findByUserIdAndCategoryIdAndYearAndMonthAndActiveTrue(userId,
                            request.getCategoryId(), request.getYear(), request.getMonth())
                    .ifPresent(b -> {
                        throw new IllegalStateException(
                                "Ya existe un presupuesto para esta categoría en ese mes");
                    });
        } else {
            // Verificar que no exista ya un presupuesto global para ese mes
            budgetRepository.findByUserIdAndCategoryIsNullAndYearAndMonthAndActiveTrue(userId,
                    request.getYear(), request.getMonth()).ifPresent(b -> {
                        throw new IllegalStateException(
                                "Ya existe un presupuesto global para ese mes");
                    });
        }

        Budget budget =
                Budget.builder().user(user).category(category).limitAmount(request.getLimitAmount())
                        .year(request.getYear()).month(request.getMonth()).active(true).build();

        return toResponse(budgetRepository.save(budget), request.getYear(), request.getMonth());
    }

    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Presupuesto no encontrado"));

        budget.setLimitAmount(request.getLimitAmount());
        return toResponse(budgetRepository.save(budget), budget.getYear(), budget.getMonth());
    }

    @Transactional
    public void deleteBudget(Long id, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Presupuesto no encontrado"));
        budget.setActive(false);
        budgetRepository.save(budget);
    }

    // ── Cálculo de gasto real y alertas ─────────────────────────────────────
    private BudgetResponse toResponse(Budget budget, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        BigDecimal spent;
        boolean isGlobal = budget.getCategory() == null;

        if (isGlobal) {
            // Gasto total del mes
            BigDecimal total = expenseRepository
                    .sumExpensesByUserAndDateRange(budget.getUser().getId(), start, end);
            spent = total != null ? total : BigDecimal.ZERO;
        } else {
            // Gasto por categoría
            BigDecimal total = expenseRepository.sumExpensesByCategory(budget.getUser().getId(),
                    budget.getCategory().getId(), start, end);
            spent = total != null ? total : BigDecimal.ZERO;
        }

        BigDecimal remaining = budget.getLimitAmount().subtract(spent);
        boolean isOverBudget = spent.compareTo(budget.getLimitAmount()) > 0;

        double usagePct = budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                ? spent.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        boolean isNearLimit = usagePct >= ALERT_THRESHOLD && !isOverBudget;

        String alertMessage = null;
        if (isOverBudget) {
            BigDecimal excess = spent.subtract(budget.getLimitAmount());
            alertMessage = "⚠️ Superaste tu presupuesto "
                    + (isGlobal ? "global" : "de " + budget.getCategory().getName()) + " por $"
                    + excess.setScale(2, RoundingMode.HALF_UP);
        } else if (isNearLimit) {
            alertMessage =
                    "🔔 Llevas el " + String.format("%.1f", usagePct) + "% de tu presupuesto "
                            + (isGlobal ? "global" : "de " + budget.getCategory().getName())
                            + ". Solo quedan $"
                            + remaining.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        return BudgetResponse.builder().id(budget.getId())
                .categoryId(isGlobal ? null : budget.getCategory().getId())
                .categoryName(isGlobal ? "Global" : budget.getCategory().getName())
                .isGlobal(isGlobal).limitAmount(budget.getLimitAmount()).spentAmount(spent)
                .remainingAmount(remaining).usagePercentage(Math.round(usagePct * 100.0) / 100.0)
                .isOverBudget(isOverBudget).isNearLimit(isNearLimit).alertMessage(alertMessage)
                .year(year).month(month).build();
    }
}
