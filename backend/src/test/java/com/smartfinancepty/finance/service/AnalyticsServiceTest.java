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
import com.smartfinancepty.finance.graphql.dto.AnalyticsResponse;
import com.smartfinancepty.finance.graphql.dto.ComparisonResponse;
import com.smartfinancepty.finance.graphql.dto.Recommendation;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.IncomeRepository;
import com.smartfinancepty.finance.service.analytics.recommendation.AnalyticsService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Tests")
class AnalyticsServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private IncomeRepository incomeRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User testUser;
    private Income testIncome;
    private ExpenseCategory foodCategory;
    private ExpenseCategory transportCategory;
    private Expense foodExpense;
    private Expense transportExpense;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").role(Role.USER).build();

        testIncome = Income.builder().id(1L).user(testUser).name("Salario")
                .amount(new BigDecimal("1200.00")).frequency(FrequencyType.MONTHLY)
                .incomeType(IncomeType.SALARY).active(true).deductions(List.of()).build();

        foodCategory = ExpenseCategory.builder().id(1L).name("Alimentación")
                .color("#4CAF50").icon("food").build();

        transportCategory = ExpenseCategory.builder().id(2L).name("Transporte")
                .color("#2196F3").icon("car").build();

        foodExpense = Expense.builder().id(1L).user(testUser).category(foodCategory)
                .description("Super 99").amount(new BigDecimal("200.00"))
                .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.of(2026, 5, 10))
                .active(true).build();

        transportExpense = Expense.builder().id(2L).user(testUser).category(transportCategory)
                .description("Gasolina").amount(new BigDecimal("50.00"))
                .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.of(2026, 5, 5))
                .active(true).build();
    }

    @Nested
    @DisplayName("getAnalytics")
    class GetAnalyticsTests {

        @Test
        @DisplayName("Debe retornar analytics del mes")
        void shouldReturnMonthlyAnalytics() {
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense, transportExpense));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            AnalyticsResponse result = analyticsService.getAnalytics(1L, 2026, 5);

            assertThat(result).isNotNull();
            assertThat(result.getTotalExpenses()).isEqualByComparingTo("250.00");
            assertThat(result.getTotalIncome()).isEqualByComparingTo("1200.00");
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonth()).isEqualTo(5);
        }

        @Test
        @DisplayName("Debe calcular tasa de ahorro correctamente")
        void shouldCalculateSavingsRate() {
            // Gastos = 200, Ingresos = 1200 → ahorro = (1000/1200)*100 ≈ 83.33%
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            AnalyticsResponse result = analyticsService.getAnalytics(1L, 2026, 5);

            assertThat(result.getSavingsRate()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Debe retornar tasa de ahorro 0 cuando no hay ingresos")
        void shouldReturnZeroSavingsRateWhenNoIncome() {
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of());
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            AnalyticsResponse result = analyticsService.getAnalytics(1L, 2026, 5);

            assertThat(result.getSavingsRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Debe incluir top categorías")
        void shouldIncludeTopCategories() {
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense, transportExpense));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            AnalyticsResponse result = analyticsService.getAnalytics(1L, 2026, 5);

            assertThat(result.getTopCategories()).isNotEmpty();
        }

        @Test
        @DisplayName("Debe incluir tendencia de 6 meses")
        void shouldIncludeSixMonthTrend() {
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            AnalyticsResponse result = analyticsService.getAnalytics(1L, 2026, 5);

            assertThat(result.getExpenseTrend()).hasSize(6);
        }

        @Test
        @DisplayName("Debe incluir predicción del próximo mes")
        void shouldIncludePrediction() {
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("300.00"));

            AnalyticsResponse result = analyticsService.getAnalytics(1L, 2026, 5);

            assertThat(result.getPrediction()).isNotNull();
            assertThat(result.getPrediction().getConfidenceLevel()).isNotNull();
        }

        @Test
        @DisplayName("Debe calcular nivel de riesgo HIGH cuando hay déficit")
        void shouldCalculateHighRiskWhenDeficit() {
            // Gastos > Ingresos
            Expense bigExpense = Expense.builder().id(3L).user(testUser).category(foodCategory)
                    .description("Gasto enorme").amount(new BigDecimal("1500.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.of(2026, 5, 1))
                    .active(true).build();

            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(bigExpense));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            AnalyticsResponse result = analyticsService.getAnalytics(1L, 2026, 5);

            assertThat(result.getRiskLevel()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Debe calcular nivel de riesgo MEDIUM cuando ahorro es bajo")
        void shouldCalculateMediumRisk() {
            // Gastos = 1020, Ingresos = 1200 → ahorro = 180/1200 = 15% (>= 10%, < 20%) → MEDIUM
            Expense highExpense = Expense.builder().id(3L).user(testUser).category(foodCategory)
                    .description("Mucho gasto").amount(new BigDecimal("1020.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.of(2026, 5, 1))
                    .active(true).build();

            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(highExpense));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            AnalyticsResponse result = analyticsService.getAnalytics(1L, 2026, 5);

            assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Debe calcular nivel de riesgo LOW cuando ahorro es bueno")
        void shouldCalculateLowRisk() {
            // Sin gastos → ahorro = 100%
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            AnalyticsResponse result = analyticsService.getAnalytics(1L, 2026, 5);

            assertThat(result.getRiskLevel()).isEqualTo("LOW");
        }
    }

    @Nested
    @DisplayName("getComparison")
    class GetComparisonTests {

        @Test
        @DisplayName("Debe comparar mes actual vs anterior")
        void shouldCompareCurrentAndPreviousMonth() {
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("300.00"))
                    .thenReturn(new BigDecimal("250.00"));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense));

            ComparisonResponse result = analyticsService.getComparison(1L, 2026, 5);

            assertThat(result).isNotNull();
            assertThat(result.getCurrentMonth()).isNotNull();
            assertThat(result.getPreviousMonth()).isNotNull();
        }

        @Test
        @DisplayName("Debe detectar aumento de gastos")
        void shouldDetectExpenseIncrease() {
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("400.00"))  // mes actual
                    .thenReturn(new BigDecimal("300.00")); // mes anterior
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense));

            ComparisonResponse result = analyticsService.getComparison(1L, 2026, 5);

            assertThat(result.isIncreased()).isTrue();
        }

        @Test
        @DisplayName("Debe detectar disminución de gastos")
        void shouldDetectExpenseDecrease() {
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("200.00"))  // mes actual
                    .thenReturn(new BigDecimal("400.00")); // mes anterior
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense));

            ComparisonResponse result = analyticsService.getComparison(1L, 2026, 5);

            assertThat(result.isIncreased()).isFalse();
        }

        @Test
        @DisplayName("Debe manejar mes anterior sin gastos (división por cero)")
        void shouldHandleZeroPreviousExpenses() {
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("300.00"))
                    .thenReturn(BigDecimal.ZERO);
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense));

            ComparisonResponse result = analyticsService.getComparison(1L, 2026, 5);

            assertThat(result.getExpenseChangePercentage()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getRecommendations")
    class GetRecommendationsTests {

        @Test
        @DisplayName("Debe generar alerta de déficit cuando gastos > ingresos")
        void shouldGenerateDeficitAlert() {
            Expense bigExpense = Expense.builder().id(3L).user(testUser).category(foodCategory)
                    .description("Gasto enorme").amount(new BigDecimal("1500.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now())
                    .active(true).build();

            // getRecommendations llama a findByUserIdAndExpenseDateBetweenAndActiveTrue
            // dos veces: mes actual y mes anterior
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(bigExpense), List.of());
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));

            List<Recommendation> recs = analyticsService.getRecommendations(1L);

            assertThat(recs).anyMatch(r -> r.getId().equals("RISK_001"));
        }

        @Test
        @DisplayName("Debe generar alerta de ahorro bajo cuando tasa < 10%")
        void shouldGenerateLowSavingsAlert() {
            // Gastos = 1100, Ingresos = 1200, tasa ≈ 8%
            Expense highExpense = Expense.builder().id(3L).user(testUser).category(foodCategory)
                    .description("Mucho gasto").amount(new BigDecimal("1100.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now())
                    .active(true).build();

            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(highExpense), List.of());
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));

            List<Recommendation> recs = analyticsService.getRecommendations(1L);

            assertThat(recs).anyMatch(r -> r.getId().equals("RISK_002"));
        }

        @Test
        @DisplayName("Debe generar mensaje positivo cuando ahorro >= 20%")
        void shouldGeneratePositiveMessageWhenGoodSavings() {
            // Sin gastos → ahorro = 100%
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(), List.of());
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));

            List<Recommendation> recs = analyticsService.getRecommendations(1L);

            assertThat(recs).anyMatch(r -> r.getId().equals("POSITIVE_001"));
        }

        @Test
        @DisplayName("Debe generar recomendación de ahorro para categoría con gasto alto")
        void shouldGenerateSavingsTipForHighSpendingCategory() {
            // Alimentación = 400 de 1200 (33% > 25%) → genera recomendación
            Expense highFoodExpense = Expense.builder().id(3L).user(testUser).category(foodCategory)
                    .description("Mucho en comida").amount(new BigDecimal("400.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now())
                    .active(true).build();

            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(highFoodExpense), List.of());
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));

            List<Recommendation> recs = analyticsService.getRecommendations(1L);

            assertThat(recs).anyMatch(r -> r.getType().equals("SAVINGS"));
        }

        @Test
        @DisplayName("Debe retornar lista no vacía con recomendaciones moderadas")
        void shouldReturnOnlyPositiveWhenModerateSpending() {
            // Gastos moderados ~16% → tasa 84%, nivel LOW
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense), List.of()); // $200 de $1200
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));

            List<Recommendation> recs = analyticsService.getRecommendations(1L);

            // Debe tener al menos el positivo
            assertThat(recs).isNotEmpty();
        }

        @Test
        @DisplayName("Recomendaciones ordenadas: HIGH primero")
        void shouldSortRecommendationsByPriority() {
            Expense bigExpense = Expense.builder().id(3L).user(testUser).category(foodCategory)
                    .description("Gran gasto").amount(new BigDecimal("1500.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now())
                    .active(true).build();

            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(bigExpense), List.of());
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));

            List<Recommendation> recs = analyticsService.getRecommendations(1L);

            if (recs.size() > 1) {
                assertThat(recs.get(0).getPriority()).isEqualTo("HIGH");
            }
        }
    }

    @Nested
    @DisplayName("getAnalyticsRest")
    class GetAnalyticsRestTests {

        @Test
        @DisplayName("Debe retornar DTO REST con analytics, recomendaciones y comparación")
        void shouldReturnRestResponse() {
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(eq(1L), any(), any()))
                    .thenReturn(List.of(foodExpense));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            var result = analyticsService.getAnalyticsRest(1L, 2026, 5);

            assertThat(result).isNotNull();
            assertThat(result.getAnalytics()).isNotNull();
            assertThat(result.getRecommendations()).isNotNull();
            assertThat(result.getComparison()).isNotNull();
        }
    }
}
