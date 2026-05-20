package com.smartfinancepty.finance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.smartfinancepty.finance.domain.*;
import com.smartfinancepty.finance.dto.SavingsGoalRequest;
import com.smartfinancepty.finance.dto.SavingsGoalResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.IncomeRepository;
import com.smartfinancepty.finance.repository.SavingsGoalRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.service.finance.SavingsGoalService;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavingsGoalService Tests")
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private SavingsGoalService savingsGoalService;

    private User testUser;
    private Income testIncome;
    private SavingsGoal fixedGoal;
    private SavingsGoal percentageGoal;
    private SavingsGoal bothGoal;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").role(Role.USER).build();

        testIncome = Income.builder().id(1L).user(testUser).name("Salario")
                .amount(new BigDecimal("1000.00")).frequency(FrequencyType.MONTHLY)
                .incomeType(IncomeType.SALARY).active(true).deductions(List.of()).build();

        fixedGoal = SavingsGoal.builder().id(1L).user(testUser).name("Emergencias")
                .fixedAmount(new BigDecimal("200.00")).active(true).build();

        percentageGoal = SavingsGoal.builder().id(2L).user(testUser).name("Vacaciones")
                .percentage(new BigDecimal("10.00")).active(true).build();

        bothGoal = SavingsGoal.builder().id(3L).user(testUser).name("Inversión")
                .fixedAmount(new BigDecimal("100.00")).percentage(new BigDecimal("15.00"))
                .active(true).build();
    }

    @Nested
    @DisplayName("Get Goals")
    class GetGoalsTests {

        @Test
        @DisplayName("Debe retornar lista de metas activas")
        void shouldReturnActiveGoals() {
            when(savingsGoalRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(fixedGoal));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("300.00"));

            List<SavingsGoalResponse> result = savingsGoalService.getAllGoals(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Emergencias");
        }

        @Test
        @DisplayName("Debe retornar metas por mes específico")
        void shouldReturnGoalsByMonth() {
            when(savingsGoalRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(fixedGoal));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            List<SavingsGoalResponse> result = savingsGoalService.getGoalsByMonth(1L, 2026, 5);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar meta por ID")
        void shouldReturnGoalById() {
            when(savingsGoalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(fixedGoal));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            SavingsGoalResponse result = savingsGoalService.getGoalById(1L, 1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Emergencias");
        }

        @Test
        @DisplayName("Debe lanzar excepción si meta no existe")
        void shouldThrowWhenGoalNotFound() {
            when(savingsGoalRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsGoalService.getGoalById(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Meta de ahorro no encontrada");
        }
    }

    @Nested
    @DisplayName("Create Goal")
    class CreateGoalTests {

        @Test
        @DisplayName("Debe crear meta con monto fijo")
        void shouldCreateGoalWithFixedAmount() {
            SavingsGoalRequest request = SavingsGoalRequest.builder().name("Emergencias")
                    .fixedAmount(new BigDecimal("200.00")).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(savingsGoalRepository.save(any())).thenReturn(fixedGoal);
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            SavingsGoalResponse result = savingsGoalService.createGoal(request, 1L);

            assertThat(result.getName()).isEqualTo("Emergencias");
            assertThat(result.getFixedAmount()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("Debe crear meta con porcentaje")
        void shouldCreateGoalWithPercentage() {
            SavingsGoalRequest request = SavingsGoalRequest.builder().name("Vacaciones")
                    .percentage(new BigDecimal("10.00")).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(savingsGoalRepository.save(any())).thenReturn(percentageGoal);
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            SavingsGoalResponse result = savingsGoalService.createGoal(request, 1L);

            assertThat(result.getPercentage()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("Debe lanzar excepción si ni monto ni porcentaje están definidos")
        void shouldThrowWhenNeitherFixedNorPercentageDefined() {
            SavingsGoalRequest request = SavingsGoalRequest.builder().name("Meta inválida").build();

            assertThatThrownBy(() -> savingsGoalService.createGoal(request, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto fijo o un porcentaje");

            verify(savingsGoalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción si usuario no existe")
        void shouldThrowWhenUserNotFound() {
            SavingsGoalRequest request = SavingsGoalRequest.builder().name("Meta")
                    .fixedAmount(new BigDecimal("200.00")).build();

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsGoalService.createGoal(request, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Goal")
    class UpdateGoalTests {

        @Test
        @DisplayName("Debe actualizar meta correctamente")
        void shouldUpdateGoalSuccessfully() {
            SavingsGoalRequest request = SavingsGoalRequest.builder().name("Actualizado")
                    .fixedAmount(new BigDecimal("300.00")).build();

            SavingsGoal updated = SavingsGoal.builder().id(1L).user(testUser).name("Actualizado")
                    .fixedAmount(new BigDecimal("300.00")).active(true).build();

            when(savingsGoalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(fixedGoal));
            when(savingsGoalRepository.save(any())).thenReturn(updated);
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            SavingsGoalResponse result = savingsGoalService.updateGoal(1L, request, 1L);

            assertThat(result.getName()).isEqualTo("Actualizado");
        }

        @Test
        @DisplayName("Debe lanzar excepción si ni monto ni porcentaje están definidos al actualizar")
        void shouldThrowWhenNeitherDefinedOnUpdate() {
            SavingsGoalRequest request = SavingsGoalRequest.builder().name("Meta").build();

            assertThatThrownBy(() -> savingsGoalService.updateGoal(1L, request, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Debe lanzar excepción si meta no existe al actualizar")
        void shouldThrowWhenGoalNotFoundOnUpdate() {
            SavingsGoalRequest request = SavingsGoalRequest.builder().name("Meta")
                    .fixedAmount(new BigDecimal("100.00")).build();

            when(savingsGoalRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsGoalService.updateGoal(99L, request, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Goal")
    class DeleteGoalTests {

        @Test
        @DisplayName("Debe hacer soft delete de la meta")
        void shouldSoftDeleteGoal() {
            when(savingsGoalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(fixedGoal));
            when(savingsGoalRepository.save(any())).thenReturn(fixedGoal);

            savingsGoalService.deleteGoal(1L, 1L);

            assertThat(fixedGoal.isActive()).isFalse();
            verify(savingsGoalRepository).save(fixedGoal);
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar meta inexistente")
        void shouldThrowWhenDeletingNonExistentGoal() {
            when(savingsGoalRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsGoalService.deleteGoal(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Goal Achievement Calculation")
    class GoalAchievementTests {

        @Test
        @DisplayName("Meta alcanzada cuando ahorro real >= meta")
        void shouldMarkGoalAsAchieved() {
            when(savingsGoalRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(fixedGoal));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            // Gastos = 700, ingreso neto = 1000, ahorro = 300 >= meta 200 → achieved
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("700.00"));

            List<SavingsGoalResponse> result = savingsGoalService.getAllGoals(1L);

            assertThat(result.get(0).isAchieved()).isTrue();
            assertThat(result.get(0).getStatusMessage()).contains("✅");
        }

        @Test
        @DisplayName("Meta no alcanzada cuando ahorro real < meta")
        void shouldMarkGoalAsNotAchieved() {
            when(savingsGoalRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(fixedGoal));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            // Gastos = 900, ingreso neto = 1000, ahorro = 100 < meta 200 → not achieved
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("900.00"));

            List<SavingsGoalResponse> result = savingsGoalService.getAllGoals(1L);

            assertThat(result.get(0).isAchieved()).isFalse();
            assertThat(result.get(0).getStatusMessage()).contains("📊");
        }

        @Test
        @DisplayName("Meta con ambos: fijo y porcentaje usa el mayor")
        void shouldUseLargerWhenBothDefined() {
            // Ingreso neto = 1000, 15% = 150, fijo = 100 → usa 150 (el mayor)
            when(savingsGoalRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(bothGoal));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("800.00"));

            List<SavingsGoalResponse> result = savingsGoalService.getAllGoals(1L);

            // ahorro = 1000 - 800 = 200, target = max(100, 150) = 150, achieved = true
            assertThat(result.get(0).getTargetAmount()).isEqualByComparingTo("150.00");
            assertThat(result.get(0).isAchieved()).isTrue();
        }

        @Test
        @DisplayName("Gastos nulos se tratan como cero")
        void shouldHandleNullExpenses() {
            when(savingsGoalRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(fixedGoal));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(null);

            List<SavingsGoalResponse> result = savingsGoalService.getAllGoals(1L);

            assertThat(result.get(0).getActualSavings()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("Sin ingresos: ahorro real es cero")
        void shouldHandleNoIncomes() {
            when(savingsGoalRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(fixedGoal));
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L)).thenReturn(List.of());
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            List<SavingsGoalResponse> result = savingsGoalService.getAllGoals(1L);

            assertThat(result.get(0).getActualSavings()).isEqualByComparingTo("0.00");
        }
    }
}
