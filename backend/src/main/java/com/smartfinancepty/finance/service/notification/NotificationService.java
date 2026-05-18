package com.smartfinancepty.finance.service.notification;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartfinancepty.finance.domain.Expense;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final EmailService emailService;
    private final PushNotificationService pushService;

    // ── Notificaciones ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toNotificationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::toNotificationResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void deleteNotification(Long id, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        notificationRepository.delete(notification);
    }

    // ── Recordatorios ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReminderResponse> getAllReminders(Long userId) {
        return reminderRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(this::toReminderResponse).toList();
    }

    @Transactional
    public ReminderResponse createReminder(ReminderRequest request, Long userId) {
        if (request.isRecurring() && request.getDayOfMonth() == null) {
            throw new IllegalArgumentException(
                    "Un recordatorio recurrente debe tener un día del mes");
        }
        if (!request.isRecurring() && request.getRemindAt() == null) {
            throw new IllegalArgumentException("Un recordatorio puntual debe tener fecha y hora");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Expense expense = null;
        if (request.getExpenseId() != null) {
            expense = expenseRepository.findByIdAndUserId(request.getExpenseId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado"));
        }

        Reminder reminder = Reminder.builder().user(user).title(request.getTitle())
                .message(request.getMessage()).channel(request.getChannel())
                .dayOfMonth(request.getDayOfMonth()).remindAt(request.getRemindAt())
                .recurring(request.isRecurring()).expense(expense).active(true).build();

        return toReminderResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public ReminderResponse updateReminder(Long id, ReminderRequest request, Long userId) {
        Reminder reminder = reminderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recordatorio no encontrado"));

        reminder.setTitle(request.getTitle());
        reminder.setMessage(request.getMessage());
        reminder.setChannel(request.getChannel());
        reminder.setDayOfMonth(request.getDayOfMonth());
        reminder.setRemindAt(request.getRemindAt());
        reminder.setRecurring(request.isRecurring());

        return toReminderResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public void deleteReminder(Long id, Long userId) {
        Reminder reminder = reminderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recordatorio no encontrado"));
        reminder.setActive(false);
        reminderRepository.save(reminder);
    }

    // ── Envío de notificaciones ───────────────────────────────────────────────

    @Transactional
    public Notification sendNotification(User user, String title, String message,
            NotificationType type, NotificationChannel channel, Long referenceId,
            String referenceType) {
        // Evitar duplicados del mismo día
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        if (referenceId != null
                && notificationRepository.existsByUserIdAndTypeAndReferenceIdAndCreatedAtAfter(
                        user.getId(), type, referenceId, startOfDay)) {
            log.info("Notificación duplicada evitada para userId={} type={} referenceId={}",
                    user.getId(), type, referenceId);
            return null;
        }

        Notification notification = Notification.builder().user(user).title(title).message(message)
                .type(type).channel(channel).referenceId(referenceId).referenceType(referenceType)
                .read(false).sent(false).scheduledAt(LocalDateTime.now()).build();

        // Enviar según canal
        if (channel == NotificationChannel.PUSH || channel == NotificationChannel.BOTH) {
            pushService.sendPush(user.getId().toString(), title, message);
        }
        if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
            emailService.sendNotificationEmail(user.getEmail(), title, message);
        }

        notification.setSent(true);
        notification.setSentAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder().id(n.getId()).title(n.getTitle())
                .message(n.getMessage()).type(n.getType()).channel(n.getChannel()).read(n.isRead())
                .sent(n.isSent()).referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType()).scheduledAt(n.getScheduledAt())
                .sentAt(n.getSentAt()).createdAt(n.getCreatedAt()).build();
    }

    private ReminderResponse toReminderResponse(Reminder r) {
        return ReminderResponse.builder().id(r.getId()).title(r.getTitle()).message(r.getMessage())
                .channel(r.getChannel()).dayOfMonth(r.getDayOfMonth()).remindAt(r.getRemindAt())
                .recurring(r.isRecurring()).active(r.isActive())
                .expenseId(r.getExpense() != null ? r.getExpense().getId() : null)
                .expenseDescription(r.getExpense() != null ? r.getExpense().getDescription() : null)
                .createdAt(r.getCreatedAt()).build();
    }
}
