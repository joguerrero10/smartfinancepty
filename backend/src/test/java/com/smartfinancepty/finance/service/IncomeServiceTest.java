package com.smartfinancepty.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
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
import com.smartfinancepty.finance.dto.DeductionRequest;
import com.smartfinancepty.finance.dto.IncomeRequest;
import com.smartfinancepty.finance.dto.IncomeResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.IncomeRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.service.finance.IncomeService;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncomeService Tests")
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IncomeService incomeService;

    private User testUser;
    private Income testIncome;
    private IncomeRequest incomeRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").fullName("Joel Guerrero")
                .role(Role.USER).build();

        testIncome = Income.builder().id(1L).user(testUser).name("Salario principal")
                .amount(new BigDecimal("1200.00")).frequency(FrequencyType.MONTHLY)
                .incomeType(IncomeType.SALARY).active(true).deductions(List.of()).build();

        incomeRequest =
                IncomeRequest.builder().name("Salario principal").amount(new BigDecimal("1200.00"))
                        .frequency(FrequencyType.MONTHLY).incomeType(IncomeType.SALARY).build();
    }

    @Nested
    @DisplayName("Get Incomes")
    class GetIncomesTests {

        @Test
        @DisplayName("Debe retornar lista de ingresos activos del usuario")
        void shouldReturnActiveIncomesForUser() {
            // Arrange
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of(testIncome));

            // Act
            List<IncomeResponse> result = incomeService.getAllIncomes(1L);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Salario principal");
            assertThat(result.get(0).getAmount()).isEqualByComparingTo("1200.00");
            assertThat(result.get(0).getNetAmount()).isEqualByComparingTo("1200.00");
            assertThat(result.get(0).getTotalDeductions()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay ingresos")
        void shouldReturnEmptyListWhenNoIncomes() {
            // Arrange
            when(incomeRepository.findByUserIdAndActiveTrueWithDeductions(1L))
                    .thenReturn(List.of());

            // Act
            List<IncomeResponse> result = incomeService.getAllIncomes(1L);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Debe lanzar excepción si el ingreso no existe")
        void shouldThrowExceptionWhenIncomeNotFound() {
            // Arrange
            when(incomeRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> incomeService.getIncomeById(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ingreso no encontrado");
        }
    }

    @Nested
    @DisplayName("Create Income")
    class CreateIncomeTests {

        @Test
        @DisplayName("Debe crear ingreso sin deducciones")
        void shouldCreateIncomeWithoutDeductions() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incomeRepository.save(any(Income.class))).thenReturn(testIncome);

            // Act
            IncomeResponse response = incomeService.createIncome(incomeRequest, 1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Salario principal");
            assertThat(response.getAmount()).isEqualByComparingTo("1200.00");
            assertThat(response.getNetAmount()).isEqualByComparingTo("1200.00");
            verify(incomeRepository).save(any(Income.class));
        }

        @Test
        @DisplayName("Debe calcular deducciones por porcentaje correctamente")
        void shouldCalculatePercentageDeductionsCorrectly() {
            // Arrange
            Deduction deduction = Deduction.builder().id(1L).name("Seguro Social")
                    .deductionType(DeductionType.SOCIAL_SECURITY).isPercentage(true)
                    .value(new BigDecimal("9.75")).build();

            Income incomeWithDeduction = Income.builder().id(1L).user(testUser).name("Salario")
                    .amount(new BigDecimal("1200.00")).frequency(FrequencyType.MONTHLY)
                    .incomeType(IncomeType.SALARY).active(true).deductions(List.of(deduction))
                    .build();

            DeductionRequest deductionRequest = DeductionRequest.builder().name("Seguro Social")
                    .deductionType(DeductionType.SOCIAL_SECURITY).isPercentage(true)
                    .value(new BigDecimal("9.75")).build();

            IncomeRequest requestWithDeduction = IncomeRequest.builder().name("Salario")
                    .amount(new BigDecimal("1200.00")).frequency(FrequencyType.MONTHLY)
                    .incomeType(IncomeType.SALARY).deductions(List.of(deductionRequest)).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incomeRepository.save(any(Income.class))).thenReturn(incomeWithDeduction);

            // Act
            IncomeResponse response = incomeService.createIncome(requestWithDeduction, 1L);

            // Assert
            // 9.75% de 1200 = 117.00
            assertThat(response.getTotalDeductions()).isEqualByComparingTo("117.00");
            assertThat(response.getNetAmount()).isEqualByComparingTo("1083.00");
        }

        @Test
        @DisplayName("Debe calcular deducciones por monto fijo correctamente")
        void shouldCalculateFixedDeductionsCorrectly() {
            // Arrange
            Deduction fixedDeduction =
                    Deduction.builder().id(1L).name("Préstamo").deductionType(DeductionType.LOAN)
                            .isPercentage(false).value(new BigDecimal("150.00")).build();

            Income incomeWithFixed = Income.builder().id(1L).user(testUser).name("Salario")
                    .amount(new BigDecimal("1200.00")).frequency(FrequencyType.MONTHLY)
                    .incomeType(IncomeType.SALARY).active(true).deductions(List.of(fixedDeduction))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(incomeRepository.save(any())).thenReturn(incomeWithFixed);

            // Act
            IncomeResponse response = incomeService.createIncome(incomeRequest, 1L);

            // Assert
            assertThat(response.getTotalDeductions()).isEqualByComparingTo("150.00");
            assertThat(response.getNetAmount()).isEqualByComparingTo("1050.00");
        }

        @Test
        @DisplayName("Debe lanzar excepción si el usuario no existe")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> incomeService.createIncome(incomeRequest, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(incomeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Income")
    class DeleteIncomeTests {

        @Test
        @DisplayName("Debe hacer soft delete del ingreso")
        void shouldSoftDeleteIncome() {
            // Arrange
            when(incomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testIncome));
            when(incomeRepository.save(any())).thenReturn(testIncome);

            // Act
            incomeService.deleteIncome(1L, 1L);

            // Assert
            assertThat(testIncome.isActive()).isFalse();
            verify(incomeRepository).save(testIncome);
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar ingreso inexistente")
        void shouldThrowExceptionWhenDeletingNonExistentIncome() {
            // Arrange
            when(incomeRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> incomeService.deleteIncome(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(incomeRepository, never()).save(any());
        }
    }
}
