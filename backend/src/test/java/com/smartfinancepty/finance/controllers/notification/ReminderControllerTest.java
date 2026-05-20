package com.smartfinancepty.finance.controllers.notification;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancepty.finance.domain.notification.NotificationChannel;
import com.smartfinancepty.finance.dto.notification.ReminderRequest;
import com.smartfinancepty.finance.dto.notification.ReminderResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.security.JwtService;
import com.smartfinancepty.finance.service.notification.NotificationService;

@WebMvcTest(ReminderController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReminderController Tests")
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private JwtService jwtService;

    private ReminderResponse reminder1;
    private ReminderRequest validRequest;

    @BeforeEach
    void setUp() {
        reminder1 = ReminderResponse.builder().id(1L).title("Pago de alquiler")
                .message("Recuerda pagar el alquiler").channel(NotificationChannel.PUSH)
                .dayOfMonth(1).recurring(true).active(true)
                .createdAt(LocalDateTime.of(2026, 5, 1, 9, 0)).build();

        validRequest = ReminderRequest.builder().title("Pago de alquiler")
                .message("Recuerda pagar el alquiler").channel(NotificationChannel.PUSH)
                .dayOfMonth(1).recurring(true).build();
    }

    @Nested
    @DisplayName("GET /api/v1/reminders")
    class GetAllRemindersTests {

        @Test
        @DisplayName("Debe retornar 200 con lista de recordatorios")
        @WithMockUser
        void shouldReturn200WithReminderList() throws Exception {
            when(notificationService.getAllReminders(any())).thenReturn(List.of(reminder1));

            mockMvc.perform(get("/api/v1/reminders")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].title").value("Pago de alquiler"))
                    .andExpect(jsonPath("$[0].recurring").value(true));
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay recordatorios")
        @WithMockUser
        void shouldReturnEmptyList() throws Exception {
            when(notificationService.getAllReminders(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/reminders")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/reminders")
    class CreateReminderTests {

        @Test
        @DisplayName("Debe retornar 201 con recordatorio creado")
        @WithMockUser
        void shouldReturn201OnCreate() throws Exception {
            when(notificationService.createReminder(any(ReminderRequest.class), any()))
                    .thenReturn(reminder1);

            mockMvc.perform(post("/api/v1/reminders").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.title").value("Pago de alquiler"));
        }

        @Test
        @DisplayName("Debe retornar 400 si el título está vacío")
        @WithMockUser
        void shouldReturn400WhenTitleIsBlank() throws Exception {
            ReminderRequest invalid = ReminderRequest.builder().title("")
                    .channel(NotificationChannel.PUSH).build();

            mockMvc.perform(post("/api/v1/reminders").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(notificationService, never()).createReminder(any(), any());
        }

        @Test
        @DisplayName("Debe retornar 400 si el canal es nulo")
        @WithMockUser
        void shouldReturn400WhenChannelIsNull() throws Exception {
            ReminderRequest invalid =
                    ReminderRequest.builder().title("Mi recordatorio").channel(null).build();

            mockMvc.perform(post("/api/v1/reminders").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 si dayOfMonth está fuera de rango")
        @WithMockUser
        void shouldReturn400WhenDayOfMonthOutOfRange() throws Exception {
            ReminderRequest invalid = ReminderRequest.builder().title("Recordatorio")
                    .channel(NotificationChannel.PUSH).dayOfMonth(32).build();

            mockMvc.perform(post("/api/v1/reminders").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/reminders/{id}")
    class UpdateReminderTests {

        @Test
        @DisplayName("Debe retornar 200 con recordatorio actualizado")
        @WithMockUser
        void shouldReturn200OnUpdate() throws Exception {
            ReminderResponse updated = ReminderResponse.builder().id(1L)
                    .title("Pago de alquiler actualizado").channel(NotificationChannel.EMAIL)
                    .dayOfMonth(5).recurring(true).active(true).build();

            when(notificationService.updateReminder(eq(1L), any(ReminderRequest.class), any()))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/v1/reminders/1").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Pago de alquiler actualizado"));
        }

        @Test
        @DisplayName("Debe retornar 404 si el recordatorio no existe")
        @WithMockUser
        void shouldReturn404WhenNotFound() throws Exception {
            when(notificationService.updateReminder(eq(99L), any(ReminderRequest.class), any()))
                    .thenThrow(new ResourceNotFoundException("Recordatorio no encontrado"));

            mockMvc.perform(put("/api/v1/reminders/99").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/reminders/{id}")
    class DeleteReminderTests {

        @Test
        @DisplayName("Debe retornar 204 al eliminar recordatorio")
        @WithMockUser
        void shouldReturn204OnDelete() throws Exception {
            doNothing().when(notificationService).deleteReminder(eq(1L), any());

            mockMvc.perform(delete("/api/v1/reminders/1").with(csrf()))
                    .andExpect(status().isNoContent());

            verify(notificationService).deleteReminder(eq(1L), any());
        }

        @Test
        @DisplayName("Debe retornar 404 si el recordatorio no existe")
        @WithMockUser
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            doThrow(new ResourceNotFoundException("Recordatorio no encontrado"))
                    .when(notificationService).deleteReminder(eq(99L), any());

            mockMvc.perform(delete("/api/v1/reminders/99").with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
