package com.smartfinancepty.finance.dto.attachment;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAttachmentResponse {
    private Long id;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String fileSizeFormatted; // "1.2 MB"
    private String fileUrl;
    private String storageProvider; // LOCAL, S3, CLOUDINARY
    private String description;
    private Long expenseId;
    private String expenseDescription;
    private LocalDateTime createdAt;
}
