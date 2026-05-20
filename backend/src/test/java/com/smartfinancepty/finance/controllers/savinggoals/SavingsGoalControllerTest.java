package com.smartfinancepty.finance.controllers.savinggoals;

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
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.SavingsGoalRequest;
import com.smartfinancepty.finance.dto.SavingsGoalResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.security.JwtService;
import com.smartfinancepty.finance.service.finance.SavingsGoalService;

@WebMvcTest(SavingsGoalController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SavingsGoalController Tests")
class SavingsGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private SavingsGoalService savingsGoalService;
    @MockitoBean
    private JwtService jwtService;

    private User testUser;
    private SavingsGoalResponse goal1;
    private SavingsGoalRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").username("joel")
                .fullName("Joel Guerrero").password("encoded").role(Role.USER).build();

        goal1 = SavingsGoalResponse.builder().id(1L).name("Fondo de emergencia")
                .fixedAmount(new BigDecimal("300.00")).targetAmount(new BigDecimal("300.00"))
                .actualSavings(new BigDecimal("1700.00")).isAchieved(true)
                .achievementPercentage(100.0).statusMessage("Meta cumplida").build();

        validRequest = SavingsGoalRequest.builder().name("Fondo de emergencia")
                .fixedAmount(new BigDecimal("300.00")).build();
    }

    @Nested
    @DisplayName("GET /api/v1/savings-goals")
    class GetAllGoalsTests {

        @Test
        @DisplayName("Debe retornar 200 con lista de metas")
        void shouldReturn200WithGoalList() throws Exception {
            when(savingsGoalService.getAllGoals(1L)).thenReturn(List.of(goal1));

            mockMvc.perform(get("/api/v1/savings-goals").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].name").value("Fondo de emergencia"));
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay metas")
        void shouldReturnEmptyList() throws Exception {
            when(savingsGoalService.getAllGoals(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/savings-goals").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/savings-goals/month")
    class GetGoalsByMonthTests {

        @Test
        @DisplayName("Debe retornar metas del mes especificado")
        void shouldReturnGoalsForMonth() throws Exception {
            when(savingsGoalService.getGoalsByMonth(1L, 2026, 5)).thenReturn(List.of(goal1));

            mockMvc.perform(get("/api/v1/savings-goals/month").with(user(testUser))
                    .param("year", "2026").param("month", "5")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/savings-goals/{id}")
    class GetGoalByIdTests {

        @Test
        @DisplayName("Debe retornar 200 con la meta")
        void shouldReturn200WithGoal() throws Exception {
            when(savingsGoalService.getGoalById(1L, 1L)).thenReturn(goal1);

            mockMvc.perform(get("/api/v1/savings-goals/1").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Fondo de emergencia"))
                    .andExpect(jsonPath("$.achieved").value(true));
        }

        @Test
        @DisplayName("Debe retornar 404 si la meta no existe")
        void shouldReturn404WhenNotFound() throws Exception {
            when(savingsGoalService.getGoalById(99L, 1L))
                    .thenThrow(new ResourceNotFoundException("Meta no encontrada"));

            mockMvc.perform(get("/api/v1/savings-goals/99").with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/savings-goals")
    class CreateGoalTests {

        @Test
        @DisplayName("Debe retornar 201 con meta creada")
        void shouldReturn201OnCreate() throws Exception {
            when(savingsGoalService.createGoal(any(SavingsGoalRequest.class), eq(1L)))
                    .thenReturn(goal1);

            mockMvc.perform(post("/api/v1/savings-goals").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Fondo de emergencia"));
        }

        @Test
        @DisplayName("Debe retornar 400 si el nombre está vacío")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            SavingsGoalRequest invalid =
                    SavingsGoalRequest.builder().name("").fixedAmount(new BigDecimal("300.00"))
                            .build();

            mockMvc.perform(post("/api/v1/savings-goals").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(savingsGoalService, never()).createGoal(any(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/savings-goals/{id}")
    class UpdateGoalTests {

        @Test
        @DisplayName("Debe retornar 200 con meta actualizada")
        void shouldReturn200OnUpdate() throws Exception {
            SavingsGoalResponse updated = SavingsGoalResponse.builder().id(1L)
                    .name("Fondo de emergencia actualizado")
                    .fixedAmount(new BigDecimal("500.00")).build();

            when(savingsGoalService.updateGoal(eq(1L), any(SavingsGoalRequest.class), eq(1L)))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/v1/savings-goals/1").with(csrf()).with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Fondo de emergencia actualizado"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/savings-goals/{id}")
    class DeleteGoalTests {

        @Test
        @DisplayName("Debe retornar 204 al eliminar meta")
        void shouldReturn204OnDelete() throws Exception {
            doNothing().when(savingsGoalService).deleteGoal(1L, 1L);

            mockMvc.perform(delete("/api/v1/savings-goals/1").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(savingsGoalService).deleteGoal(1L, 1L);
        }

        @Test
        @DisplayName("Debe retornar 404 si la meta no existe")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            doThrow(new ResourceNotFoundException("Meta no encontrada")).when(savingsGoalService)
                    .deleteGoal(99L, 1L);

            mockMvc.perform(delete("/api/v1/savings-goals/99").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }
}
