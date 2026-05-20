package com.smartfinancepty.finance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
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
import com.smartfinancepty.finance.domain.Expense;
import com.smartfinancepty.finance.domain.ExpenseCategory;
import com.smartfinancepty.finance.domain.ExpenseType;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.domain.notification.Notification;
import com.smartfinancepty.finance.domain.notification.NotificationChannel;
import com.smartfinancepty.finance.domain.notification.NotificationType;
import com.smartfinancepty.finance.domain.notification.Reminder;
import com.smartfinancepty.finance.dto.notification.NotificationResponse;
import com.smartfinancepty.finance.dto.notification.ReminderRequest;
import com.smartfinancepty.finance.dto.notification.ReminderResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.notification.EmailService;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.repository.notification.NotificationRepository;
import com.smartfinancepty.finance.repository.notification.ReminderRepository;
import com.smartfinancepty.finance.service.notification.NotificationService;
import com.smartfinancepty.finance.service.notification.PushNotificationService;

import java.math.BigDecimal;
import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PushNotificationService pushService;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;
    private Reminder testReminder;
    private ExpenseCategory testCategory;
    private Expense testExpense;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("joel@smartfinance.com").role(Role.USER).build();

        testCategory = ExpenseCategory.builder().id(1L).name("Transporte").build();

        testExpense = Expense.builder().id(1L).user(testUser).category(testCategory)
                .description("Gasolina").amount(new BigDecimal("50.00"))
                .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now()).active(true).build();

        testNotification = Notification.builder().id(1L).user(testUser).title("Alerta de presupuesto")
                .message("Has alcanzado el 80% de tu presupuesto")
                .type(NotificationType.BUDGET_NEAR_LIMIT).channel(NotificationChannel.PUSH)
                .read(false).sent(true).build();

        testReminder = Reminder.builder().id(1L).user(testUser).title("Pago de agua")
                .message("Recuerda pagar el agua").channel(NotificationChannel.EMAIL)
                .dayOfMonth(15).recurring(true).active(true).build();
    }

    @Nested
    @DisplayName("Get Notifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("Debe retornar todas las notificaciones del usuario")
        void shouldReturnAllNotifications() {
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(testNotification));

            List<NotificationResponse> result = notificationService.getAllNotifications(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Alerta de presupuesto");
        }

        @Test
        @DisplayName("Debe retornar notificaciones no leídas")
        void shouldReturnUnreadNotifications() {
            when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(testNotification));

            List<NotificationResponse> result = notificationService.getUnreadNotifications(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isRead()).isFalse();
        }

        @Test
        @DisplayName("Debe retornar conteo de notificaciones no leídas")
        void shouldReturnUnreadCount() {
            when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(5L);

            long count = notificationService.getUnreadCount(1L);

            assertThat(count).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Mark Notifications")
    class MarkNotificationsTests {

        @Test
        @DisplayName("Debe marcar notificación como leída")
        void shouldMarkNotificationAsRead() {
            when(notificationRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testNotification));
            when(notificationRepository.save(any())).thenReturn(testNotification);

            notificationService.markAsRead(1L, 1L);

            assertThat(testNotification.isRead()).isTrue();
            verify(notificationRepository).save(testNotification);
        }

        @Test
        @DisplayName("Debe lanzar excepción si notificación no existe al marcar")
        void shouldThrowWhenNotificationNotFound() {
            when(notificationRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Notificación no encontrada");
        }

        @Test
        @DisplayName("Debe marcar todas las notificaciones como leídas")
        void shouldMarkAllAsRead() {
            notificationService.markAllAsRead(1L);

            verify(notificationRepository).markAllAsReadByUserId(1L);
        }
    }

    @Nested
    @DisplayName("Delete Notification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Debe eliminar notificación correctamente")
        void shouldDeleteNotification() {
            when(notificationRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testNotification));

            notificationService.deleteNotification(1L, 1L);

            verify(notificationRepository).delete(testNotification);
        }

        @Test
        @DisplayName("Debe lanzar excepción si notificación no existe al eliminar")
        void shouldThrowWhenDeletingNonExistentNotification() {
            when(notificationRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.deleteNotification(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Reminders")
    class ReminderTests {

        @Test
        @DisplayName("Debe retornar recordatorios activos")
        void shouldReturnActiveReminders() {
            when(reminderRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(testReminder));

            List<ReminderResponse> result = notificationService.getAllReminders(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Pago de agua");
        }

        @Test
        @DisplayName("Debe crear recordatorio recurrente con día del mes")
        void shouldCreateRecurringReminder() {
            ReminderRequest request = ReminderRequest.builder().title("Pago de agua")
                    .message("Recuerda pagar").channel(NotificationChannel.EMAIL)
                    .dayOfMonth(15).recurring(true).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(reminderRepository.save(any())).thenReturn(testReminder);

            ReminderResponse result = notificationService.createReminder(request, 1L);

            assertThat(result.getTitle()).isEqualTo("Pago de agua");
            assertThat(result.isRecurring()).isTrue();
        }

        @Test
        @DisplayName("Debe crear recordatorio puntual con fecha y hora")
        void shouldCreateOneTimeReminder() {
            ReminderRequest request = ReminderRequest.builder().title("Reunión")
                    .channel(NotificationChannel.PUSH)
                    .remindAt(LocalDateTime.now().plusDays(1)).recurring(false).build();

            Reminder oneTimeReminder = Reminder.builder().id(2L).user(testUser).title("Reunión")
                    .channel(NotificationChannel.PUSH).recurring(false).active(true).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(reminderRepository.save(any())).thenReturn(oneTimeReminder);

            ReminderResponse result = notificationService.createReminder(request, 1L);

            assertThat(result.isRecurring()).isFalse();
        }

        @Test
        @DisplayName("Debe crear recordatorio con gasto asociado")
        void shouldCreateReminderWithExpense() {
            ReminderRequest request = ReminderRequest.builder().title("Cuota del carro")
                    .channel(NotificationChannel.EMAIL)
                    .dayOfMonth(5).recurring(true).expenseId(1L).build();

            Reminder reminderWithExpense = Reminder.builder().id(3L).user(testUser)
                    .title("Cuota del carro").channel(NotificationChannel.EMAIL)
                    .dayOfMonth(5).recurring(true).expense(testExpense).active(true).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testExpense));
            when(reminderRepository.save(any())).thenReturn(reminderWithExpense);

            ReminderResponse result = notificationService.createReminder(request, 1L);

            assertThat(result.getExpenseDescription()).isEqualTo("Gasolina");
        }

        @Test
        @DisplayName("Debe lanzar excepción si recurrente sin día del mes")
        void shouldThrowWhenRecurringWithoutDayOfMonth() {
            ReminderRequest request = ReminderRequest.builder().title("Recordatorio")
                    .channel(NotificationChannel.PUSH).recurring(true).build();

            assertThatThrownBy(() -> notificationService.createReminder(request, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("día del mes");
        }

        @Test
        @DisplayName("Debe lanzar excepción si puntual sin fecha y hora")
        void shouldThrowWhenOneTimeWithoutRemindAt() {
            ReminderRequest request = ReminderRequest.builder().title("Recordatorio")
                    .channel(NotificationChannel.PUSH).recurring(false).build();

            assertThatThrownBy(() -> notificationService.createReminder(request, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha y hora");
        }

        @Test
        @DisplayName("Debe actualizar recordatorio")
        void shouldUpdateReminder() {
            ReminderRequest request = ReminderRequest.builder().title("Actualizado")
                    .channel(NotificationChannel.EMAIL).dayOfMonth(20).recurring(true).build();

            Reminder updated = Reminder.builder().id(1L).user(testUser).title("Actualizado")
                    .channel(NotificationChannel.EMAIL).dayOfMonth(20).recurring(true).active(true)
                    .build();

            when(reminderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testReminder));
            when(reminderRepository.save(any())).thenReturn(updated);

            ReminderResponse result = notificationService.updateReminder(1L, request, 1L);

            assertThat(result.getTitle()).isEqualTo("Actualizado");
        }

        @Test
        @DisplayName("Debe lanzar excepción si recordatorio no existe al actualizar")
        void shouldThrowWhenReminderNotFoundOnUpdate() {
            ReminderRequest request = ReminderRequest.builder().title("Test")
                    .channel(NotificationChannel.PUSH).recurring(false)
                    .remindAt(LocalDateTime.now().plusHours(1)).build();

            when(reminderRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.updateReminder(99L, request, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recordatorio no encontrado");
        }

        @Test
        @DisplayName("Debe hacer soft delete de recordatorio")
        void shouldSoftDeleteReminder() {
            when(reminderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testReminder));
            when(reminderRepository.save(any())).thenReturn(testReminder);

            notificationService.deleteReminder(1L, 1L);

            assertThat(testReminder.isActive()).isFalse();
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar recordatorio inexistente")
        void shouldThrowWhenDeletingNonExistentReminder() {
            when(reminderRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.deleteReminder(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Send Notification")
    class SendNotificationTests {

        @Test
        @DisplayName("Debe enviar notificación por PUSH")
        void shouldSendPushNotification() {
            when(notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAtAfter(
                    any(), any(), any(), any())).thenReturn(false);
            when(notificationRepository.save(any())).thenReturn(testNotification);

            Notification result = notificationService.sendNotification(testUser, "Alerta",
                    "Mensaje", NotificationType.BUDGET_NEAR_LIMIT, NotificationChannel.PUSH, 1L, "BUDGET");

            assertThat(result).isNotNull();
            verify(pushService).sendPush(eq("1"), any(), any());
            verify(emailService, never()).sendNotificationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("Debe enviar notificación por EMAIL")
        void shouldSendEmailNotification() {
            when(notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAtAfter(
                    any(), any(), any(), any())).thenReturn(false);
            when(notificationRepository.save(any())).thenReturn(testNotification);

            notificationService.sendNotification(testUser, "Alerta", "Mensaje",
                    NotificationType.BUDGET_NEAR_LIMIT, NotificationChannel.EMAIL, 1L, "BUDGET");

            verify(emailService).sendNotificationEmail(any(), any(), any());
            verify(pushService, never()).sendPush(any(), any(), any());
        }

        @Test
        @DisplayName("Debe enviar notificación por BOTH (push y email)")
        void shouldSendBothNotification() {
            when(notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAtAfter(
                    any(), any(), any(), any())).thenReturn(false);
            when(notificationRepository.save(any())).thenReturn(testNotification);

            notificationService.sendNotification(testUser, "Alerta", "Mensaje",
                    NotificationType.BUDGET_NEAR_LIMIT, NotificationChannel.BOTH, 1L, "BUDGET");

            verify(pushService).sendPush(any(), any(), any());
            verify(emailService).sendNotificationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("Debe evitar notificaciones duplicadas del mismo día")
        void shouldAvoidDuplicateNotifications() {
            when(notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAtAfter(
                    any(), any(), any(), any())).thenReturn(true);

            Notification result = notificationService.sendNotification(testUser, "Alerta",
                    "Mensaje", NotificationType.BUDGET_NEAR_LIMIT, NotificationChannel.PUSH, 1L, "BUDGET");

            assertThat(result).isNull();
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe enviar siempre cuando referenceId es null")
        void shouldAlwaysSendWhenReferenceIdIsNull() {
            when(notificationRepository.save(any())).thenReturn(testNotification);

            Notification result = notificationService.sendNotification(testUser, "Info",
                    "Mensaje", NotificationType.BUDGET_NEAR_LIMIT, NotificationChannel.PUSH, null, null);

            assertThat(result).isNotNull();
        }
    }
}
