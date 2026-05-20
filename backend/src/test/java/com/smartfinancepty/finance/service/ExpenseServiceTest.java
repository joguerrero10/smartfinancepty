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
import com.smartfinancepty.finance.dto.ExpenseRequest;
import com.smartfinancepty.finance.dto.ExpenseResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.ExpenseCategoryRepository;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.ExpenseSubCategoryRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.service.finance.ExpenseService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Tests")
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseCategoryRepository categoryRepository;
    @Mock
    private ExpenseSubCategoryRepository subCategoryRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User testUser;
    private ExpenseCategory testCategory;
    private ExpenseSubCategory testSubCategory;
    private Expense testExpense;
    private ExpenseRequest expenseRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").role(Role.USER).build();

        testCategory = ExpenseCategory.builder().id(1L).name("Alimentación").build();

        testSubCategory = ExpenseSubCategory.builder().id(1L).name("Supermercado")
                .category(testCategory).build();

        testExpense = Expense.builder().id(1L).user(testUser).category(testCategory)
                .description("Compra en Super 99").amount(new BigDecimal("85.50"))
                .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.of(2026, 5, 15))
                .active(true).build();

        expenseRequest = ExpenseRequest.builder().description("Compra en Super 99")
                .amount(new BigDecimal("85.50")).categoryId(1L)
                .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.of(2026, 5, 15)).build();
    }

    @Nested
    @DisplayName("Get Expenses")
    class GetExpensesTests {

        @Test
        @DisplayName("Debe retornar lista de gastos activos del usuario")
        void shouldReturnActiveExpensesForUser() {
            when(expenseRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(testExpense));

            List<ExpenseResponse> result = expenseService.getAllExpenses(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("Compra en Super 99");
            assertThat(result.get(0).getAmount()).isEqualByComparingTo("85.50");
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay gastos")
        void shouldReturnEmptyListWhenNoExpenses() {
            when(expenseRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of());

            List<ExpenseResponse> result = expenseService.getAllExpenses(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Debe retornar gastos por rango de fechas")
        void shouldReturnExpensesByDateRange() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            LocalDate end = LocalDate.of(2026, 5, 31);
            when(expenseRepository.findByUserIdAndExpenseDateBetweenAndActiveTrue(1L, start, end))
                    .thenReturn(List.of(testExpense));

            List<ExpenseResponse> result = expenseService.getExpensesByDateRange(1L, start, end);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar gastos por tipo")
        void shouldReturnExpensesByType() {
            when(expenseRepository.findByUserIdAndExpenseTypeAndActiveTrue(1L, ExpenseType.VARIABLE))
                    .thenReturn(List.of(testExpense));

            List<ExpenseResponse> result = expenseService.getExpensesByType(1L, ExpenseType.VARIABLE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getExpenseType()).isEqualTo(ExpenseType.VARIABLE);
        }

        @Test
        @DisplayName("Debe retornar gasto por ID")
        void shouldReturnExpenseById() {
            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testExpense));

            ExpenseResponse result = expenseService.getExpenseById(1L, 1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDescription()).isEqualTo("Compra en Super 99");
        }

        @Test
        @DisplayName("Debe lanzar excepción si el gasto no existe")
        void shouldThrowExceptionWhenExpenseNotFound() {
            when(expenseRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.getExpenseById(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Gasto no encontrado");
        }

        @Test
        @DisplayName("Debe retornar la entidad del gasto con getExpenseEntity")
        void shouldReturnExpenseEntityById() {
            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testExpense));

            Expense result = expenseService.getExpenseEntity(1L, 1L);

            assertThat(result).isEqualTo(testExpense);
        }

        @Test
        @DisplayName("getExpenseEntity debe lanzar excepción si el gasto no existe")
        void shouldThrowExceptionInGetExpenseEntityWhenNotFound() {
            when(expenseRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.getExpenseEntity(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Create Expense")
    class CreateExpenseTests {

        @Test
        @DisplayName("Debe crear gasto sin subcategoría")
        void shouldCreateExpenseWithoutSubCategory() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(expenseRepository.save(any())).thenReturn(testExpense);

            ExpenseResponse result = expenseService.createExpense(expenseRequest, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getDescription()).isEqualTo("Compra en Super 99");
            assertThat(result.getSubCategoryName()).isNull();
            verify(expenseRepository).save(any(Expense.class));
        }

        @Test
        @DisplayName("Debe crear gasto con subcategoría")
        void shouldCreateExpenseWithSubCategory() {
            ExpenseRequest requestWithSub = ExpenseRequest.builder()
                    .description("Compra en Super 99").amount(new BigDecimal("85.50"))
                    .categoryId(1L).subCategoryId(1L).expenseType(ExpenseType.VARIABLE)
                    .expenseDate(LocalDate.of(2026, 5, 15)).build();

            Expense expenseWithSub = Expense.builder().id(1L).user(testUser).category(testCategory)
                    .subCategory(testSubCategory).description("Compra en Super 99")
                    .amount(new BigDecimal("85.50")).expenseType(ExpenseType.VARIABLE)
                    .expenseDate(LocalDate.of(2026, 5, 15)).active(true).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(testSubCategory));
            when(expenseRepository.save(any())).thenReturn(expenseWithSub);

            ExpenseResponse result = expenseService.createExpense(requestWithSub, 1L);

            assertThat(result.getSubCategoryName()).isEqualTo("Supermercado");
        }

        @Test
        @DisplayName("Debe lanzar excepción si usuario no existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.createExpense(expenseRequest, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");

            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción si categoría no existe")
        void shouldThrowWhenCategoryNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.createExpense(expenseRequest, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Categoría no encontrada");
        }

        @Test
        @DisplayName("Debe lanzar excepción si subcategoría no existe")
        void shouldThrowWhenSubCategoryNotFound() {
            ExpenseRequest requestWithSub = ExpenseRequest.builder()
                    .description("Compra").amount(new BigDecimal("10.00"))
                    .categoryId(1L).subCategoryId(99L).expenseType(ExpenseType.VARIABLE)
                    .expenseDate(LocalDate.now()).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(subCategoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.createExpense(requestWithSub, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Subcategoría no encontrada");
        }
    }

    @Nested
    @DisplayName("Update Expense")
    class UpdateExpenseTests {

        @Test
        @DisplayName("Debe actualizar gasto correctamente")
        void shouldUpdateExpenseSuccessfully() {
            ExpenseRequest updateRequest = ExpenseRequest.builder()
                    .description("Actualizado").amount(new BigDecimal("100.00"))
                    .categoryId(1L).expenseType(ExpenseType.FIXED)
                    .expenseDate(LocalDate.now()).build();

            Expense updatedExpense = Expense.builder().id(1L).user(testUser).category(testCategory)
                    .description("Actualizado").amount(new BigDecimal("100.00"))
                    .expenseType(ExpenseType.FIXED).expenseDate(LocalDate.now()).active(true).build();

            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testExpense));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(expenseRepository.save(any())).thenReturn(updatedExpense);

            ExpenseResponse result = expenseService.updateExpense(1L, updateRequest, 1L);

            assertThat(result.getDescription()).isEqualTo("Actualizado");
            assertThat(result.getAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("Debe lanzar excepción si gasto no existe al actualizar")
        void shouldThrowWhenExpenseNotFoundOnUpdate() {
            when(expenseRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.updateExpense(99L, expenseRequest, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Debe lanzar excepción si categoría no existe al actualizar")
        void shouldThrowWhenCategoryNotFoundOnUpdate() {
            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testExpense));
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.updateExpense(1L, expenseRequest, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Expense")
    class DeleteExpenseTests {

        @Test
        @DisplayName("Debe hacer soft delete del gasto")
        void shouldSoftDeleteExpense() {
            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testExpense));
            when(expenseRepository.save(any())).thenReturn(testExpense);

            expenseService.deleteExpense(1L, 1L);

            assertThat(testExpense.isActive()).isFalse();
            verify(expenseRepository).save(testExpense);
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar gasto inexistente")
        void shouldThrowWhenDeletingNonExistentExpense() {
            when(expenseRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.deleteExpense(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(expenseRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("toResponse con subcategoría null")
    class ToResponseTests {

        @Test
        @DisplayName("Debe mapear correctamente cuando hay subcategoría")
        void shouldMapSubCategoryName() {
            Expense expenseWithSub = Expense.builder().id(1L).user(testUser).category(testCategory)
                    .subCategory(testSubCategory).description("Test").amount(new BigDecimal("50.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now()).active(true)
                    .build();

            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(expenseWithSub));

            ExpenseResponse result = expenseService.getExpenseById(1L, 1L);

            assertThat(result.getSubCategoryName()).isEqualTo("Supermercado");
            assertThat(result.getCategoryName()).isEqualTo("Alimentación");
        }
    }
}
