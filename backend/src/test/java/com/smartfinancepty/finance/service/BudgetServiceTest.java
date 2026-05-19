package com.smartfinancepty.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.smartfinancepty.finance.domain.Budget;
import com.smartfinancepty.finance.domain.ExpenseCategory;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.BudgetRequest;
import com.smartfinancepty.finance.dto.BudgetResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.BudgetRepository;
import com.smartfinancepty.finance.repository.ExpenseCategoryRepository;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.service.finance.BudgetService;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService Tests")
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseCategoryRepository categoryRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private ExpenseCategory testCategory;
    private Budget globalBudget;
    private Budget categoryBudget;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").role(Role.USER).build();

        testCategory = ExpenseCategory.builder().id(1L).name("Hogar").color("#4A90D9").icon("home")
                .build();

        globalBudget = Budget.builder().id(1L).user(testUser).category(null)
                .limitAmount(new BigDecimal("800.00")).year(2026).month(5).active(true).build();

        categoryBudget = Budget.builder().id(2L).user(testUser).category(testCategory)
                .limitAmount(new BigDecimal("200.00")).year(2026).month(5).active(true).build();
    }

    @Nested
    @DisplayName("Create Budget")
    class CreateBudgetTests {

        @Test
        @DisplayName("Debe crear presupuesto global exitosamente")
        void shouldCreateGlobalBudgetSuccessfully() {
            // Arrange
            BudgetRequest request = BudgetRequest.builder().limitAmount(new BigDecimal("800.00"))
                    .year(2026).month(5).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(budgetRepository.findByUserIdAndCategoryIsNullAndYearAndMonthAndActiveTrue(1L,
                    2026, 5)).thenReturn(Optional.empty());
            when(expenseRepository.sumExpensesByUserAndDateRange(any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(budgetRepository.save(any())).thenReturn(globalBudget);

            // Act
            BudgetResponse response = budgetService.createBudget(request, 1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.isGlobal()).isTrue();
            assertThat(response.getLimitAmount()).isEqualByComparingTo("800.00");
            assertThat(response.getCategoryName()).isEqualTo("Global");
        }

        @Test
        @DisplayName("Debe crear presupuesto por categoría exitosamente")
        void shouldCreateCategoryBudgetSuccessfully() {
            // Arrange
            BudgetRequest request = BudgetRequest.builder().categoryId(1L)
                    .limitAmount(new BigDecimal("200.00")).year(2026).month(5).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(budgetRepository.findByUserIdAndCategoryIdAndYearAndMonthAndActiveTrue(1L, 1L,
                    2026, 5)).thenReturn(Optional.empty());
            when(expenseRepository.sumExpensesByCategory(any(), any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(budgetRepository.save(any())).thenReturn(categoryBudget);

            // Act
            BudgetResponse response = budgetService.createBudget(request, 1L);

            // Assert
            assertThat(response.isGlobal()).isFalse();
            assertThat(response.getCategoryName()).isEqualTo("Hogar");
            assertThat(response.getLimitAmount()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("Debe lanzar excepción si ya existe presupuesto global para ese mes")
        void shouldThrowExceptionWhenGlobalBudgetAlreadyExists() {
            // Arrange
            BudgetRequest request = BudgetRequest.builder().limitAmount(new BigDecimal("800.00"))
                    .year(2026).month(5).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(budgetRepository.findByUserIdAndCategoryIsNullAndYearAndMonthAndActiveTrue(1L,
                    2026, 5)).thenReturn(Optional.of(globalBudget));

            // Act & Assert
            assertThatThrownBy(() -> budgetService.createBudget(request, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Ya existe un presupuesto global");
        }
    }

    @Nested
    @DisplayName("Budget Alerts")
    class BudgetAlertTests {

        @Test
        @DisplayName("Debe detectar presupuesto al 80% y generar alerta")
        void shouldDetectBudgetNearLimit() {
            // Arrange — gastado $680 de $800 = 85%
            when(budgetRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(globalBudget));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("680.00"));

            // Act
            BudgetResponse response = budgetService.getBudgetById(1L, 1L);

            // Assert
            assertThat(response.isNearLimit()).isTrue();
            assertThat(response.isOverBudget()).isFalse();
            assertThat(response.getUsagePercentage()).isGreaterThanOrEqualTo(80.0);
            assertThat(response.getAlertMessage()).isNotNull();
            assertThat(response.getAlertMessage()).contains("85");
        }

        @Test
        @DisplayName("Debe detectar presupuesto superado")
        void shouldDetectOverBudget() {
            // Arrange — gastado $900 de $800 = 112.5%
            when(budgetRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(globalBudget));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("900.00"));

            // Act
            BudgetResponse response = budgetService.getBudgetById(1L, 1L);

            // Assert
            assertThat(response.isOverBudget()).isTrue();
            assertThat(response.isNearLimit()).isFalse();
            assertThat(response.getAlertMessage()).contains("⚠️");
            assertThat(response.getRemainingAmount()).isNegative();
        }

        @Test
        @DisplayName("No debe generar alerta si el gasto es menor al 80%")
        void shouldNotGenerateAlertWhenUnder80Percent() {
            // Arrange — gastado $400 de $800 = 50%
            when(budgetRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(globalBudget));
            when(expenseRepository.sumExpensesByUserAndDateRange(eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("400.00"));

            // Act
            BudgetResponse response = budgetService.getBudgetById(1L, 1L);

            // Assert
            assertThat(response.isNearLimit()).isFalse();
            assertThat(response.isOverBudget()).isFalse();
            assertThat(response.getAlertMessage()).isNull();
            assertThat(response.getUsagePercentage()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Debe calcular monto restante correctamente")
        void shouldCalculateRemainingAmountCorrectly() {
            // Arrange — gastado $150 de $200
            when(budgetRepository.findByIdAndUserId(2L, 1L))
                    .thenReturn(Optional.of(categoryBudget));
            when(expenseRepository.sumExpensesByCategory(eq(1L), eq(1L), any(), any()))
                    .thenReturn(new BigDecimal("150.00"));

            // Act
            BudgetResponse response = budgetService.getBudgetById(2L, 1L);

            // Assert
            assertThat(response.getSpentAmount()).isEqualByComparingTo("150.00");
            assertThat(response.getRemainingAmount()).isEqualByComparingTo("50.00");
            assertThat(response.getUsagePercentage()).isEqualTo(75.0);
        }
    }

    @Nested
    @DisplayName("Delete Budget")
    class DeleteBudgetTests {

        @Test
        @DisplayName("Debe hacer soft delete del presupuesto")
        void shouldSoftDeleteBudget() {
            // Arrange
            when(budgetRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(globalBudget));
            when(budgetRepository.save(any())).thenReturn(globalBudget);

            // Act
            budgetService.deleteBudget(1L, 1L);

            // Assert
            assertThat(globalBudget.isActive()).isFalse();
            verify(budgetRepository).save(globalBudget);
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar presupuesto inexistente")
        void shouldThrowExceptionWhenDeletingNonExistentBudget() {
            // Arrange
            when(budgetRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> budgetService.deleteBudget(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
