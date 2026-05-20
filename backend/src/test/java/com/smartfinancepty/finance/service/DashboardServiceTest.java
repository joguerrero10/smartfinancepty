package com.smartfinancepty.finance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.smartfinancepty.finance.domain.*;
import com.smartfinancepty.finance.graphql.dto.DashboardResponse;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.IncomeRepository;
import com.smartfinancepty.finance.service.analytics.dashboard.DashboardService;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;
    private Income testIncome;
    private ExpenseCategory foodCategory;
    private ExpenseCategory transportCategory;
    private Expense fixedExpense;
    private Expense variableExpense;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").role(Role.USER).build();

        testIncome = Income.builder().id(1L).user(testUser).name("Salario")
                .amount(new BigDecimal("1500.00")).frequency(FrequencyType.MONTHLY)
                .incomeType(IncomeType.SALARY).active(true).deductions(List.of()).build();

        foodCategory = ExpenseCategory.builder().id(1L).name("Alimentación")
                .color("#4CAF50").icon("food").build();

        transportCategory = ExpenseCategory.builder().id(2L).name("Transporte")
                .color("#2196F3").icon("car").build();

        fixedExpense = Expense.builder().id(1L).user(testUser).category(foodCategory)
                .description("Mercado mensual").amount(new BigDecimal("300.00"))
                .expenseType(ExpenseType.FIXED).expenseDate(LocalDate.of(2026, 5, 1)).active(true)
                .build();

        variableExpense = Expense.builder().id(2L).user(testUser).category(transportCategory)
                .description("Taxi").amount(new BigDecimal("25.00"))
                .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.of(2026, 5, 10))
                .active(true).build();
    }

    @Nested
    @DisplayName("getDashboard")
    class GetDashboardTests {

        @Test
        @DisplayName("Debe retornar dashboard del mes actual")
        void shouldReturnCurrentMonthDashboard() {
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(fixedExpense, variableExpense));

            DashboardResponse result = dashboardService.getDashboard(1L);

            assertThat(result).isNotNull();
            assertThat(result.getTotalGrossIncome()).isEqualByComparingTo("1500.00");
            assertThat(result.getTotalNetIncome()).isEqualByComparingTo("1500.00");
            assertThat(result.getTotalExpensesMonth()).isEqualByComparingTo("325.00");
            assertThat(result.getBalance()).isEqualByComparingTo("1175.00");
        }

        @Test
        @DisplayName("Debe calcular separado fijos y variables")
        void shouldSeparateFixedAndVariableExpenses() {
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(fixedExpense, variableExpense));

            DashboardResponse result = dashboardService.getDashboardByMonth(1L, 2026, 5);

            assertThat(result.getTotalFixedExpenses()).isEqualByComparingTo("300.00");
            assertThat(result.getTotalVariableExpenses()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("Debe calcular deducciones de ingresos")
        void shouldCalculateDeductionsFromIncome() {
            Deduction deduction = Deduction.builder().id(1L).name("CSS")
                    .deductionType(DeductionType.SOCIAL_SECURITY).isPercentage(true)
                    .value(new BigDecimal("9.75")).build();

            Income incomeWithDeduction = Income.builder().id(1L).user(testUser).name("Salario")
                    .amount(new BigDecimal("1500.00")).frequency(FrequencyType.MONTHLY)
                    .incomeType(IncomeType.SALARY).active(true).deductions(List.of(deduction))
                    .build();

            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(incomeWithDeduction));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of());

            DashboardResponse result = dashboardService.getDashboardByMonth(1L, 2026, 5);

            // 9.75% de 1500 = 146.25, neto = 1353.75
            assertThat(result.getTotalNetIncome()).isEqualByComparingTo("1353.75");
            assertThat(result.getTotalDeductions()).isEqualByComparingTo("146.25");
        }

        @Test
        @DisplayName("Debe calcular gastos por categoría")
        void shouldGroupExpensesByCategory() {
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(fixedExpense, variableExpense));

            DashboardResponse result = dashboardService.getDashboardByMonth(1L, 2026, 5);

            assertThat(result.getExpensesByCategory()).hasSize(2);
        }

        @Test
        @DisplayName("Debe retornar los últimos 5 gastos ordenados por fecha")
        void shouldReturnRecentExpenses() {
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(fixedExpense, variableExpense));

            DashboardResponse result = dashboardService.getDashboardByMonth(1L, 2026, 5);

            assertThat(result.getRecentExpenses()).hasSizeLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Debe calcular tasa de ahorro correctamente")
        void shouldCalculateSavingsRate() {
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            // Sin gastos → ahorro = 100%
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of());

            DashboardResponse result = dashboardService.getDashboardByMonth(1L, 2026, 5);

            assertThat(result.getSavingsPercentage()).isEqualTo(100.0);
            assertThat(result.getSavingsProjected()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("Debe manejar dashboard sin ingresos ni gastos")
        void shouldHandleEmptyDashboard() {
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of());
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of());

            DashboardResponse result = dashboardService.getDashboardByMonth(1L, 2026, 5);

            assertThat(result.getTotalGrossIncome()).isEqualByComparingTo("0.00");
            assertThat(result.getTotalExpensesMonth()).isEqualByComparingTo("0.00");
            assertThat(result.getSavingsPercentage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Debe lanzar excepción si mes inválido")
        void shouldThrowWhenInvalidMonth() {
            assertThatThrownBy(() -> dashboardService.getDashboardByMonth(1L, 2026, 13))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mes inválido");
        }

        @Test
        @DisplayName("Debe lanzar excepción si año inválido")
        void shouldThrowWhenInvalidYear() {
            assertThatThrownBy(() -> dashboardService.getDashboardByMonth(1L, 1999, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Año inválido");
        }

        @Test
        @DisplayName("Debe incluir gastos con subcategoría en recentExpenses")
        void shouldIncludeSubCategoryInRecentExpenses() {
            ExpenseSubCategory sub = ExpenseSubCategory.builder().id(1L)
                    .category(foodCategory).name("Frutas").build();

            Expense expenseWithSub = Expense.builder().id(3L).user(testUser).category(foodCategory)
                    .subCategory(sub).description("Frutas").amount(new BigDecimal("20.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.of(2026, 5, 5))
                    .active(true).build();

            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(expenseWithSub));

            DashboardResponse result = dashboardService.getDashboardByMonth(1L, 2026, 5);

            assertThat(result.getRecentExpenses()).isNotEmpty();
            assertThat(result.getRecentExpenses().get(0).getSubCategoryName()).isEqualTo("Frutas");
        }
    }
}
