package com.smartfinancepty.finance.notification;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    /**
     * Envía un email de notificación. En producción integrar con SendGrid, AWS SES o
     * JavaMailSender. Por ahora loggea el intento para desarrollo.
     */
    public void sendNotificationEmail(String toEmail, String subject, String body) {
        log.info("📧 EMAIL → To: {} | Subject: {} | Body: {}", toEmail, subject, body);
        // TODO Fase 4: integrar con proveedor de email
        // Ejemplo con Spring Mail:
        // SimpleMailMessage message = new SimpleMailMessage();
        // message.setTo(toEmail);
        // message.setSubject(subject);
        // message.setText(body);
        // mailSender.send(message);
    }
}
