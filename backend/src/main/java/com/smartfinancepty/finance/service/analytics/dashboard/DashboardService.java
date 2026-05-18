package com.smartfinancepty.finance.service.analytics.dashboard;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartfinancepty.finance.domain.*;
import com.smartfinancepty.finance.graphql.dto.CategorySummary;
import com.smartfinancepty.finance.graphql.dto.DashboardResponse;
import com.smartfinancepty.finance.graphql.dto.IncomeSummary;
import com.smartfinancepty.finance.graphql.dto.RecentExpense;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId) {
        LocalDate now = LocalDate.now();
        return buildDashboard(userId, now.getYear(), now.getMonthValue());
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardByMonth(Long userId, int year, int month) {
        return buildDashboard(userId, year, month);
    }

    @Transactional(readOnly = true)
    private DashboardResponse buildDashboard(Long userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException(
                    "Mes inválido: " + month + ". Debe estar entre 1 y 12");
        }
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Año inválido: " + year);
        }
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // ── Ingresos ─────────────────────────────────────────────────────────
        List<Income> incomes = incomeRepository.findByUserIdAndActiveTrueWithDeductions(userId);

        List<IncomeSummary> incomeSummaries = incomes.stream().map(income -> {
            List<Deduction> deductions = income.getDeductions();
            BigDecimal totalDed = deductions.stream()
                    .map(d -> d.isPercentage()
                            ? income.getAmount().multiply(d.getValue())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            : d.getValue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return IncomeSummary.builder().id(income.getId()).name(income.getName())
                    .incomeType(income.getIncomeType().name()).grossAmount(income.getAmount())
                    .netAmount(income.getAmount().subtract(totalDed)).totalDeductions(totalDed)
                    .frequency(income.getFrequency().name()).build();
        }).toList();

        BigDecimal totalGross = incomeSummaries.stream().map(IncomeSummary::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNet = incomeSummaries.stream().map(IncomeSummary::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDeducciones = totalGross.subtract(totalNet);

        // ── Gastos del mes ───────────────────────────────────────────────────
        List<Expense> expenses = expenseRepository
                .findByUserIdAndExpenseDateBetweenAndActiveTrue(userId, start, end);

        BigDecimal totalFixed =
                expenses.stream().filter(e -> e.getExpenseType() == ExpenseType.FIXED)
                        .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVariable =
                expenses.stream().filter(e -> e.getExpenseType() == ExpenseType.VARIABLE)
                        .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = totalFixed.add(totalVariable);

        // ── Balance y ahorro ─────────────────────────────────────────────────
        BigDecimal balance = totalNet.subtract(totalExpenses);
        BigDecimal savingsProjected = balance.max(BigDecimal.ZERO);

        double savingsPercentage = totalNet.compareTo(BigDecimal.ZERO) > 0
                ? savingsProjected.divide(totalNet, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // ── Gastos por categoría ─────────────────────────────────────────────
        Map<ExpenseCategory, List<Expense>> byCategory =
                expenses.stream().collect(Collectors.groupingBy(Expense::getCategory));

        List<CategorySummary> categorySummaries = byCategory.entrySet().stream().map(entry -> {
            ExpenseCategory cat = entry.getKey();
            List<Expense> catExpenses = entry.getValue();
            BigDecimal catTotal = catExpenses.stream().map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            double pct = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                    ? catTotal.divide(totalExpenses, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            return CategorySummary.builder().categoryId(cat.getId()).categoryName(cat.getName())
                    .totalAmount(catTotal).percentage(pct).expenseCount(catExpenses.size())
                    .color(cat.getColor()).icon(cat.getIcon()).build();
        }).sorted(Comparator.comparing(CategorySummary::getTotalAmount).reversed()).toList();

        // ── Últimos 5 gastos ─────────────────────────────────────────────────
        List<RecentExpense> recentExpenses = expenses.stream()
                .sorted(Comparator.comparing(Expense::getExpenseDate).reversed()).limit(5)
                .map(e -> RecentExpense.builder().id(e.getId()).description(e.getDescription())
                        .amount(e.getAmount()).categoryName(e.getCategory().getName())
                        .subCategoryName(
                                e.getSubCategory() != null ? e.getSubCategory().getName() : null)
                        .expenseType(e.getExpenseType().name()).expenseDate(e.getExpenseDate())
                        .build())
                .toList();

        return DashboardResponse.builder().totalGrossIncome(totalGross).totalNetIncome(totalNet)
                .totalDeductions(totalDeducciones).totalExpensesMonth(totalExpenses)
                .totalFixedExpenses(totalFixed).totalVariableExpenses(totalVariable)
                .balance(balance).savingsProjected(savingsProjected)
                .savingsPercentage(savingsPercentage).month(month).year(year)
                .incomeCount(incomes.size()).expenseCount(expenses.size())
                .expensesByCategory(categorySummaries).recentExpenses(recentExpenses)
                .incomes(incomeSummaries).build();
    }

}
