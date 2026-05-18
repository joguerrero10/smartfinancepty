package com.smartfinancepty.finance.domain.notification;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import com.smartfinancepty.finance.domain.Expense;
import com.smartfinancepty.finance.domain.User;
import lombok.*;

@Entity
@Table(name = "reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    // Día del mes para recordatorios recurrentes (1-31)
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    // Para recordatorios puntuales
    @Column(name = "remind_at")
    private LocalDateTime remindAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean recurring = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Referencia al gasto fijo relacionado (opcional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
