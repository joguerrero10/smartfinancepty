package com.smartfinancepty.finance.controllers.notification;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.notification.ReminderRequest;
import com.smartfinancepty.finance.dto.notification.ReminderResponse;
import com.smartfinancepty.finance.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "Reminders", description = "Gestión de recordatorios")
@SecurityRequirement(name = "bearerAuth")
public class ReminderController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Obtener todos los recordatorios activos")
    public ResponseEntity<List<ReminderResponse>> getAllReminders(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getAllReminders(user.getId()));
    }

    @PostMapping
    @Operation(summary = "Crear recordatorio")
    public ResponseEntity<ReminderResponse> createReminder(
            @Valid @RequestBody ReminderRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.createReminder(request, user.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar recordatorio")
    public ResponseEntity<ReminderResponse> updateReminder(@PathVariable Long id,
            @Valid @RequestBody ReminderRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.updateReminder(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar recordatorio (soft delete)")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        notificationService.deleteReminder(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
