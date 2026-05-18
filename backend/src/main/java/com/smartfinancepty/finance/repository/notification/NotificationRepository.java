package com.smartfinancepty.finance.repository.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.smartfinancepty.finance.domain.notification.Notification;
import com.smartfinancepty.finance.domain.notification.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllAsReadByUserId(Long userId);

    // Para el cron - evitar duplicados del mismo día
    boolean existsByUserIdAndTypeAndReferenceIdAndCreatedAtAfter(Long userId, NotificationType type,
            Long referenceId, java.time.LocalDateTime after);

}
