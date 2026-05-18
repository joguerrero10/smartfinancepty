package com.smartfinancepty.finance.graphql.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    // Ingresos
    private BigDecimal totalGrossIncome;
    private BigDecimal totalNetIncome;
    private BigDecimal totalDeductions;

    // Gastos del mes
    private BigDecimal totalExpensesMonth;
    private BigDecimal totalFixedExpenses;
    private BigDecimal totalVariableExpenses;

    // Balance y ahorro
    private BigDecimal balance;
    private BigDecimal savingsProjected;
    private double savingsPercentage;

    // Período
    private int month;
    private int year;

    // Conteos
    private int incomeCount;
    private int expenseCount;

    // Desglose
    private List<CategorySummary> expensesByCategory;
    private List<RecentExpense> recentExpenses;
    private List<IncomeSummary> incomes;

}
