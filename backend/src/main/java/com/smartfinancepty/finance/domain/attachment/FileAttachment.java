package com.smartfinancepty.finance.domain.attachment;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import com.smartfinancepty.finance.domain.Expense;
import com.smartfinancepty.finance.domain.User;
import lombok.*;

@Entity
@Table(name = "file_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Gasto asociado (opcional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename; // UUID + extensión

    @Column(nullable = false)
    private String contentType; // image/jpeg, application/pdf, etc.

    @Column(nullable = false)
    private Long fileSize; // bytes

    @Column(nullable = false)
    private String storageProvider; // LOCAL, S3, CLOUDINARY

    @Column(nullable = false)
    private String fileUrl; // URL pública o ruta local

    @Column
    private String storageKey; // key en S3 o public_id en Cloudinary

    @Column
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
