package com.smartfinancepty.finance.dto.notification;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.smartfinancepty.finance.domain.notification.NotificationChannel;
import lombok.*;

// ── ReminderRequest ───────────────────────────────────────────────────────────
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderRequest {

    @NotBlank(message = "El título es requerido")
    private String title;

    private String message;

    @NotNull(message = "El canal es requerido")
    private NotificationChannel channel;

    // Para recurrente: día del mes (1-31)
    @Min(value = 1, message = "El día debe estar entre 1 y 31")
    @Max(value = 31, message = "El día debe estar entre 1 y 31")
    private Integer dayOfMonth;

    // Para puntual: fecha y hora exacta
    private LocalDateTime remindAt;

    private boolean recurring;

    // Gasto fijo asociado (opcional)
    private Long expenseId;

}
