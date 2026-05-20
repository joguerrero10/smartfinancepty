package com.smartfinancepty.finance.controllers.income;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancepty.finance.domain.FrequencyType;
import com.smartfinancepty.finance.domain.IncomeType;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.IncomeRequest;
import com.smartfinancepty.finance.dto.IncomeResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.security.JwtService;
import com.smartfinancepty.finance.service.finance.IncomeService;

@WebMvcTest(IncomeController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("IncomeController Tests")
class IncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private IncomeService incomeService;
    @MockitoBean
    private JwtService jwtService;

    private User testUser;
    private IncomeResponse income1;
    private IncomeRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").username("joel")
                .fullName("Joel Guerrero").password("encoded").role(Role.USER).build();

        income1 = IncomeResponse.builder().id(1L).name("Salario mensual")
                .amount(new BigDecimal("2000.00")).netAmount(new BigDecimal("1700.00"))
                .totalDeductions(new BigDecimal("300.00")).frequency(FrequencyType.MONTHLY)
                .incomeType(IncomeType.SALARY).active(true).build();

        validRequest = IncomeRequest.builder().name("Salario mensual")
                .amount(new BigDecimal("2000.00")).frequency(FrequencyType.MONTHLY)
                .incomeType(IncomeType.SALARY).build();
    }

    @Nested
    @DisplayName("GET /api/v1/incomes")
    class GetAllIncomesTests {

        @Test
        @DisplayName("Debe retornar 200 con lista de ingresos")
        void shouldReturn200WithIncomeList() throws Exception {
            when(incomeService.getAllIncomes(1L)).thenReturn(List.of(income1));

            mockMvc.perform(get("/api/v1/incomes").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].name").value("Salario mensual"));
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay ingresos")
        void shouldReturnEmptyList() throws Exception {
            when(incomeService.getAllIncomes(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/incomes").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/incomes/{id}")
    class GetIncomeByIdTests {

        @Test
        @DisplayName("Debe retornar 200 con el ingreso")
        void shouldReturn200WithIncome() throws Exception {
            when(incomeService.getIncomeById(1L, 1L)).thenReturn(income1);

            mockMvc.perform(get("/api/v1/incomes/1").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Salario mensual"))
                    .andExpect(jsonPath("$.amount").value(2000.00));
        }

        @Test
        @DisplayName("Debe retornar 404 si el ingreso no existe")
        void shouldReturn404WhenNotFound() throws Exception {
            when(incomeService.getIncomeById(99L, 1L))
                    .thenThrow(new ResourceNotFoundException("Ingreso no encontrado"));

            mockMvc.perform(get("/api/v1/incomes/99").with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/incomes")
    class CreateIncomeTests {

        @Test
        @DisplayName("Debe retornar 201 con ingreso creado")
        void shouldReturn201OnCreate() throws Exception {
            when(incomeService.createIncome(any(IncomeRequest.class), eq(1L))).thenReturn(income1);

            mockMvc.perform(post("/api/v1/incomes").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Salario mensual"));
        }

        @Test
        @DisplayName("Debe retornar 400 si el nombre está vacío")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            IncomeRequest invalid = IncomeRequest.builder().name("").amount(new BigDecimal("1000.00"))
                    .frequency(FrequencyType.MONTHLY).incomeType(IncomeType.SALARY).build();

            mockMvc.perform(post("/api/v1/incomes").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(incomeService, never()).createIncome(any(), any());
        }

        @Test
        @DisplayName("Debe retornar 400 si el monto es nulo")
        void shouldReturn400WhenAmountIsNull() throws Exception {
            IncomeRequest invalid = IncomeRequest.builder().name("Salario").amount(null)
                    .frequency(FrequencyType.MONTHLY).incomeType(IncomeType.SALARY).build();

            mockMvc.perform(post("/api/v1/incomes").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/incomes/{id}")
    class UpdateIncomeTests {

        @Test
        @DisplayName("Debe retornar 200 con ingreso actualizado")
        void shouldReturn200OnUpdate() throws Exception {
            IncomeResponse updated = IncomeResponse.builder().id(1L).name("Salario actualizado")
                    .amount(new BigDecimal("2500.00")).frequency(FrequencyType.MONTHLY)
                    .incomeType(IncomeType.SALARY).active(true).build();

            when(incomeService.updateIncome(eq(1L), any(IncomeRequest.class), eq(1L)))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/v1/incomes/1").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Salario actualizado"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/incomes/{id}")
    class DeleteIncomeTests {

        @Test
        @DisplayName("Debe retornar 204 al eliminar ingreso")
        void shouldReturn204OnDelete() throws Exception {
            doNothing().when(incomeService).deleteIncome(1L, 1L);

            mockMvc.perform(delete("/api/v1/incomes/1").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(incomeService).deleteIncome(1L, 1L);
        }

        @Test
        @DisplayName("Debe retornar 404 si el ingreso no existe")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            doThrow(new ResourceNotFoundException("Ingreso no encontrado")).when(incomeService)
                    .deleteIncome(99L, 1L);

            mockMvc.perform(delete("/api/v1/incomes/99").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }
}
