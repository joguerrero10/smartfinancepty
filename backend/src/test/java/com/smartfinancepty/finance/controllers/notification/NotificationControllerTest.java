package com.smartfinancepty.finance.controllers.notification;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.domain.notification.NotificationChannel;
import com.smartfinancepty.finance.domain.notification.NotificationType;
import com.smartfinancepty.finance.dto.notification.NotificationResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.security.JwtService;
import com.smartfinancepty.finance.service.notification.NotificationService;

@WebMvcTest(NotificationController.class)
@DisplayName("NotificationController Tests")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private JwtService jwtService;

    private User testUser;
    private NotificationResponse notification1;
    private NotificationResponse notification2;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").username("joel")
                .fullName("Joel Guerrero").password("encoded").role(Role.USER).build();

        notification1 = NotificationResponse.builder().id(1L).title("Alerta de presupuesto")
                .message("Has superado el 80% de tu presupuesto de Alimentación")
                .type(NotificationType.BUDGET_NEAR_LIMIT).channel(NotificationChannel.PUSH)
                .read(false).sent(true).createdAt(LocalDateTime.of(2026, 5, 10, 9, 0)).build();

        notification2 = NotificationResponse.builder().id(2L).title("Recordatorio de pago")
                .message("Pago de alquiler vence mañana").type(NotificationType.EXPENSE_DUE)
                .channel(NotificationChannel.EMAIL).read(true).sent(true)
                .createdAt(LocalDateTime.of(2026, 5, 9, 8, 0)).build();
    }

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetAllNotificationsTests {

        @Test
        @DisplayName("Debe retornar 200 con todas las notificaciones")
        void shouldReturn200WithAllNotifications() throws Exception {
            when(notificationService.getAllNotifications(1L))
                    .thenReturn(List.of(notification1, notification2));

            mockMvc.perform(get("/api/v1/notifications").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].title").value("Alerta de presupuesto"))
                    .andExpect(jsonPath("$[1].read").value(true));
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay notificaciones")
        void shouldReturnEmptyList() throws Exception {
            when(notificationService.getAllNotifications(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/notifications").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/unread")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("Debe retornar solo notificaciones no leídas")
        void shouldReturnOnlyUnreadNotifications() throws Exception {
            when(notificationService.getUnreadNotifications(1L)).thenReturn(List.of(notification1));

            mockMvc.perform(get("/api/v1/notifications/unread").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].read").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/unread/count")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Debe retornar el conteo de notificaciones no leídas")
        void shouldReturnUnreadCount() throws Exception {
            when(notificationService.getUnreadCount(1L)).thenReturn(3L);

            mockMvc.perform(get("/api/v1/notifications/unread/count").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(3));
        }

        @Test
        @DisplayName("Debe retornar cero cuando no hay notificaciones no leídas")
        void shouldReturnZeroWhenNoUnread() throws Exception {
            when(notificationService.getUnreadCount(1L)).thenReturn(0L);

            mockMvc.perform(get("/api/v1/notifications/unread/count").with(user(testUser)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/{id}/read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Debe retornar 204 al marcar como leída")
        void shouldReturn204OnMarkAsRead() throws Exception {
            doNothing().when(notificationService).markAsRead(1L, 1L);

            mockMvc.perform(patch("/api/v1/notifications/1/read").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(notificationService).markAsRead(1L, 1L);
        }

        @Test
        @DisplayName("Debe retornar 404 si la notificación no existe")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Notificación no encontrada"))
                    .when(notificationService).markAsRead(99L, 1L);

            mockMvc.perform(
                    patch("/api/v1/notifications/99/read").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/read-all")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Debe retornar 204 al marcar todas como leídas")
        void shouldReturn204OnMarkAllAsRead() throws Exception {
            doNothing().when(notificationService).markAllAsRead(1L);

            mockMvc.perform(
                    patch("/api/v1/notifications/read-all").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(notificationService).markAllAsRead(1L);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/notifications/{id}")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Debe retornar 204 al eliminar notificación")
        void shouldReturn204OnDelete() throws Exception {
            doNothing().when(notificationService).deleteNotification(1L, 1L);

            mockMvc.perform(delete("/api/v1/notifications/1").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNoContent());

            verify(notificationService).deleteNotification(1L, 1L);
        }

        @Test
        @DisplayName("Debe retornar 404 si la notificación no existe")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            doThrow(new ResourceNotFoundException("Notificación no encontrada"))
                    .when(notificationService).deleteNotification(99L, 1L);

            mockMvc.perform(delete("/api/v1/notifications/99").with(csrf()).with(user(testUser)))
                    .andExpect(status().isNotFound());
        }
    }
}
