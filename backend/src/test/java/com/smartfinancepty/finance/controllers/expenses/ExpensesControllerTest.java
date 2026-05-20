package com.smartfinancepty.finance.controllers.expenses;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancepty.finance.domain.ExpenseType;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.ExpenseRequest;
import com.smartfinancepty.finance.dto.ExpenseResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.security.JwtService;
import com.smartfinancepty.finance.service.finance.ExpenseService;

@WebMvcTest(ExpensesController.class)
@DisplayName("ExpensesController Tests")
class ExpensesControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private ExpenseService expenseService;
    @MockitoBean
    private JwtService jwtService;

    private User testUser;
    private ExpenseResponse expense1;
    private ExpenseResponse expense2;
    private ExpenseRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").username("joel")
                .fullName("Joel Guerrero").password("encoded").role(Role.USER).build();

        expense1 = ExpenseResponse.builder().id(1L).description("Supermercado")
                .amount(new BigDecimal("150.00")).expenseType(ExpenseType.VARIABLE)
                .categoryName("Alimentación").expenseDate(LocalDate.of(2026, 5, 1)).active(true)
                .build();

        expense2 = ExpenseResponse.builder().id(2L).description("Alquiler")
                .amount(new BigDecimal("800.00")).expenseType(ExpenseType.FIXED)
                .categoryName("Vivienda").expenseDate(LocalDate.of(2026, 5, 1)).active(true)
                .build();

        validRequest = ExpenseRequest.builder().description("Supermercado")
                .amount(new BigDecimal("150.00")).categoryId(1L).expenseType(ExpenseType.VARIABLE)
                .expenseDate(LocalDate.of(2026, 5, 10)).build();
    }

    @Nested
    @DisplayName("GET /api/v1/expenses")
    class GetAllExpensesTests {

        @Test
        @DisplayName("Debe retornar 200 con lista de gastos")
        void shouldReturn200WithExpenseList() throws Exception {
            when(expenseService.getAllExpenses(1L)).thenReturn(List.of(expense1, expense2));

            mockMvc.perform(get("/api/v1/expenses").with(user(testUser))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].description").value("Supermercado"))
                    .andExpect(jsonPath("$[1].id").value(2L));
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay gastos")
        void shouldReturnEmptyListWhenNoExpenses() throws Exception {
            when(expenseService.getAllExpenses(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/expenses").with(user(testUser))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/{id}")
    class GetExpenseByIdTests {

        @Test
        @DisplayName("Debe retornar 200 con el gasto")
        void shouldReturn200WithExpense() throws Exception {
            when(expenseService.getExpenseById(1L, 1L)).thenReturn(expense1);

            mockMvc.perform(get("/api/v1/expenses/1").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.description").value("Supermercado"))
                    .andExpect(jsonPath("$.amount").value(150.00));
        }

        @Test
        @DisplayName("Debe retornar 404 si el gasto no existe")
        void shouldReturn404WhenNotFound() throws Exception {
            when(expenseService.getExpenseById(99L, 1L))
                    .thenThrow(new ResourceNotFoundException("Gasto no encontrado"));

            mockMvc.perform(get("/api/v1/expenses/99").with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/range")
    class GetExpensesByDateRangeTests {

        @Test
        @DisplayName("Debe retornar gastos dentro del rango de fechas")
        void shouldReturnExpensesInDateRange() throws Exception {
            when(expenseService.getExpensesByDateRange(eq(1L), any(), any()))
                    .thenReturn(List.of(expense1));

            mockMvc.perform(get("/api/v1/expenses/range").with(user(testUser))
                    .param("start", "2026-05-01").param("end", "2026-05-31"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/expenses/type/{type}")
    class GetExpensesByTypeTests {

        @Test
        @DisplayName("Debe retornar gastos variables")
        void shouldReturnVariableExpenses() throws Exception {
            when(expenseService.getExpensesByType(1L, ExpenseType.VARIABLE))
                    .thenReturn(List.of(expense1));

            mockMvc.perform(get("/api/v1/expenses/type/VARIABLE").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].expenseType").value("VARIABLE"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/expenses")
    class CreateExpenseTests {

        @Test
        @DisplayName("Debe retornar 201 con gasto creado")
        void shouldReturn201OnCreate() throws Exception {
            when(expenseService.createExpense(any(ExpenseRequest.class), eq(1L)))
                    .thenReturn(expense1);

            mockMvc.perform(post("/api/v1/expenses").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.description").value("Supermercado"));
        }

        @Test
        @DisplayName("Debe retornar 400 si la descripción está vacía")
        void shouldReturn400WhenDescriptionIsBlank() throws Exception {
            ExpenseRequest invalid = ExpenseRequest.builder().description("")
                    .amount(new BigDecimal("100.00")).categoryId(1L)
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now()).build();

            mockMvc.perform(post("/api/v1/expenses").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(expenseService, never()).createExpense(any(), any());
        }

        @Test
        @DisplayName("Debe retornar 400 si el monto es cero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            ExpenseRequest invalid = ExpenseRequest.builder().description("Test")
                    .amount(BigDecimal.ZERO).categoryId(1L).expenseType(ExpenseType.VARIABLE)
                    .expenseDate(LocalDate.now()).build();

            mockMvc.perform(post("/api/v1/expenses").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/expenses/{id}")
    class UpdateExpenseTests {

        @Test
        @DisplayName("Debe retornar 200 con gasto actualizado")
        void shouldReturn200OnUpdate() throws Exception {
            ExpenseResponse updated = ExpenseResponse.builder().id(1L)
                    .description("Supermercado actualizado").amount(new BigDecimal("200.00"))
                    .expenseType(ExpenseType.VARIABLE).categoryName("Alimentación")
                    .expenseDate(LocalDate.now()).active(true).build();

            when(expenseService.updateExpense(eq(1L), any(ExpenseRequest.class), eq(1L)))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/v1/expenses/1").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Supermercado actualizado"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/expenses/{id}")
    class DeleteExpenseTests {

        @Test
        @DisplayName("Debe retornar 204 al eliminar gasto")
        void shouldReturn204OnDelete() throws Exception {
            doNothing().when(expenseService).deleteExpense(1L, 1L);

            mockMvc.perform(delete("/api/v1/expenses/1").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(expenseService).deleteExpense(1L, 1L);
        }

        @Test
        @DisplayName("Debe retornar 404 si el gasto no existe")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            doThrow(new ResourceNotFoundException("Gasto no encontrado")).when(expenseService)
                    .deleteExpense(99L, 1L);

            mockMvc.perform(delete("/api/v1/expenses/99").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }
}
