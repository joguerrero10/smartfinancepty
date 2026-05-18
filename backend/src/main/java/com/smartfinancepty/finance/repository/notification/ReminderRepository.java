package com.smartfinancepty.finance.repository.notification;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smartfinancepty.finance.domain.notification.Reminder;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserIdAndActiveTrue(Long userId);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    // Para el cron: recordatorios del día actual
    List<Reminder> findByActiveTrueAndDayOfMonth(Integer dayOfMonth);

    // Para recordatorios puntuales pendientes
    List<Reminder> findByActiveTrueAndRecurringFalseAndRemindAtBetween(LocalDateTime start,
            LocalDateTime end);
}
