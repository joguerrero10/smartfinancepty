package com.smartfinancepty.finance.controllers.budget;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
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
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.BudgetRequest;
import com.smartfinancepty.finance.dto.BudgetResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.security.JwtService;
import com.smartfinancepty.finance.service.finance.BudgetService;

@WebMvcTest(BudgetController.class)
@DisplayName("BudgetController Tests")
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private BudgetService budgetService;
    @MockitoBean
    private JwtService jwtService;

    private User testUser;
    private BudgetResponse budget1;
    private BudgetRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").username("joel")
                .fullName("Joel Guerrero").password("encoded").role(Role.USER).build();

        budget1 = BudgetResponse.builder().id(1L).categoryName("Alimentación")
                .limitAmount(new BigDecimal("500.00")).spentAmount(new BigDecimal("320.00"))
                .remainingAmount(new BigDecimal("180.00")).usagePercentage(64.0).isOverBudget(false)
                .isNearLimit(false).year(2026).month(5).build();

        validRequest = BudgetRequest.builder().categoryId(1L).limitAmount(new BigDecimal("500.00"))
                .year(2026).month(5).build();
    }

    @Nested
    @DisplayName("GET /api/v1/budgets")
    class GetCurrentMonthBudgetsTests {

        @Test
        @DisplayName("Debe retornar 200 con presupuestos del mes actual")
        void shouldReturn200WithCurrentMonthBudgets() throws Exception {
            when(budgetService.getBudgetsByMonth(eq(1L), anyInt(), anyInt()))
                    .thenReturn(List.of(budget1));

            mockMvc.perform(get("/api/v1/budgets").with(user(testUser))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].categoryName").value("Alimentación"));
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay presupuestos")
        void shouldReturnEmptyList() throws Exception {
            when(budgetService.getBudgetsByMonth(eq(1L), anyInt(), anyInt())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/budgets").with(user(testUser))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/budgets/month")
    class GetBudgetsByMonthTests {

        @Test
        @DisplayName("Debe retornar presupuestos del mes especificado")
        void shouldReturnBudgetsForSpecificMonth() throws Exception {
            when(budgetService.getBudgetsByMonth(1L, 2026, 4)).thenReturn(List.of(budget1));

            mockMvc.perform(get("/api/v1/budgets/month").with(user(testUser)).param("year", "2026")
                    .param("month", "4")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/budgets/{id}")
    class GetBudgetByIdTests {

        @Test
        @DisplayName("Debe retornar 200 con el presupuesto")
        void shouldReturn200WithBudget() throws Exception {
            when(budgetService.getBudgetById(1L, 1L)).thenReturn(budget1);

            mockMvc.perform(get("/api/v1/budgets/1").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.limitAmount").value(500.00))
                    .andExpect(jsonPath("$.usagePercentage").value(64.0));
        }

        @Test
        @DisplayName("Debe retornar 404 si el presupuesto no existe")
        void shouldReturn404WhenNotFound() throws Exception {
            when(budgetService.getBudgetById(99L, 1L))
                    .thenThrow(new ResourceNotFoundException("Presupuesto no encontrado"));

            mockMvc.perform(get("/api/v1/budgets/99").with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/budgets")
    class CreateBudgetTests {

        @Test
        @DisplayName("Debe retornar 201 con presupuesto creado")
        void shouldReturn201OnCreate() throws Exception {
            when(budgetService.createBudget(any(BudgetRequest.class), eq(1L))).thenReturn(budget1);

            mockMvc.perform(post("/api/v1/budgets").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("Debe retornar 400 si el monto límite es nulo")
        void shouldReturn400WhenLimitAmountIsNull() throws Exception {
            BudgetRequest invalid = BudgetRequest.builder().categoryId(1L).limitAmount(null)
                    .year(2026).month(5).build();

            mockMvc.perform(post("/api/v1/budgets").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(budgetService, never()).createBudget(any(), any());
        }

        @Test
        @DisplayName("Debe retornar 400 si el mes está fuera de rango")
        void shouldReturn400WhenMonthIsOutOfRange() throws Exception {
            BudgetRequest invalid = BudgetRequest.builder().limitAmount(new BigDecimal("500.00"))
                    .year(2026).month(13).build();

            mockMvc.perform(post("/api/v1/budgets").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/budgets/{id}")
    class UpdateBudgetTests {

        @Test
        @DisplayName("Debe retornar 200 con presupuesto actualizado")
        void shouldReturn200OnUpdate() throws Exception {
            BudgetResponse updated = BudgetResponse.builder().id(1L).categoryName("Alimentación")
                    .limitAmount(new BigDecimal("600.00")).year(2026).month(5).build();

            when(budgetService.updateBudget(eq(1L), any(BudgetRequest.class), eq(1L)))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/v1/budgets/1").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.limitAmount").value(600.00));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/budgets/{id}")
    class DeleteBudgetTests {

        @Test
        @DisplayName("Debe retornar 204 al eliminar presupuesto")
        void shouldReturn204OnDelete() throws Exception {
            doNothing().when(budgetService).deleteBudget(1L, 1L);

            mockMvc.perform(delete("/api/v1/budgets/1").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(budgetService).deleteBudget(1L, 1L);
        }

        @Test
        @DisplayName("Debe retornar 404 si el presupuesto no existe")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            doThrow(new ResourceNotFoundException("Presupuesto no encontrado")).when(budgetService)
                    .deleteBudget(99L, 1L);

            mockMvc.perform(delete("/api/v1/budgets/99").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }
}
