package com.smartfinancepty.finance.service.upload;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.smartfinancepty.finance.domain.Expense;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.domain.attachment.FileAttachment;
import com.smartfinancepty.finance.dto.attachment.FileAttachmentResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.repository.attachment.FileAttachmentRepository;
import com.smartfinancepty.finance.service.storage.StorageResult;
import com.smartfinancepty.finance.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileAttachmentService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final StorageService storageService;
    private final String messageAttachmentNotFound = "Archivo adjunto no encontrado";
    private final String messageBillsNotFound = "Gasto no encontrado";

    @Value("${app.upload.max-size-mb:10}")
    private long maxSizeMb;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp",
            "image/gif", "application/pdf", "text/xml", "application/xml",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel");

    // ── Upload ────────────────────────────────────────────────────────────────

    @Transactional
    public FileAttachmentResponse uploadFile(MultipartFile file, Long expenseId, String description,
            Long userId) {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Expense expense = null;
        if (expenseId != null) {
            expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException(messageBillsNotFound));
        }

        String folder = "receipts/" + userId;
        StorageResult result = storageService.upload(file, folder);

        FileAttachment attachment = FileAttachment.builder().user(user).expense(expense)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(result.getStoredFilename()).contentType(file.getContentType())
                .fileSize(file.getSize()).storageProvider(result.getProvider())
                .fileUrl(result.getFileUrl()).storageKey(result.getStorageKey())
                .description(description).active(true).build();

        return toResponse(fileAttachmentRepository.save(attachment));
    }

    // ── Get ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FileAttachmentResponse> getAllFiles(Long userId) {
        return fileAttachmentRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FileAttachmentResponse> getFilesByExpense(Long expenseId, Long userId) {
        // Verificar que el gasto pertenece al usuario
        expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageBillsNotFound));
        return fileAttachmentRepository.findByExpenseIdAndActiveTrue(expenseId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FileAttachmentResponse> getUnlinkedFiles(Long userId) {
        return fileAttachmentRepository.findByUserIdAndExpenseIsNullAndActiveTrue(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FileAttachmentResponse getFileById(Long id, Long userId) {
        return toResponse(fileAttachmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageAttachmentNotFound)));
    }

    // ── Asociar a gasto ───────────────────────────────────────────────────────

    @Transactional
    public FileAttachmentResponse linkToExpense(Long fileId, Long expenseId, Long userId) {
        FileAttachment attachment = fileAttachmentRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageAttachmentNotFound));

        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageBillsNotFound));

        attachment.setExpense(expense);
        return toResponse(fileAttachmentRepository.save(attachment));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteFile(Long id, Long userId) {
        FileAttachment attachment = fileAttachmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageAttachmentNotFound));

        // Eliminar del storage
        storageService.delete(attachment.getStorageKey());

        // Soft delete en DB
        attachment.setActive(false);
        fileAttachmentRepository.save(attachment);

        log.info("🗑️ Archivo eliminado: {} (provider: {})", attachment.getOriginalFilename(),
                attachment.getStorageProvider());
    }

    // ── Validaciones ──────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        long maxBytes = maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "El archivo supera el tamaño máximo de " + maxSizeMb + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido: " + contentType
                    + ". Tipos permitidos: " + String.join(", ", ALLOWED_TYPES));
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private FileAttachmentResponse toResponse(FileAttachment f) {
        return FileAttachmentResponse.builder().id(f.getId())
                .originalFilename(f.getOriginalFilename()).contentType(f.getContentType())
                .fileSize(f.getFileSize()).fileSizeFormatted(formatSize(f.getFileSize()))
                .fileUrl(f.getFileUrl()).storageProvider(f.getStorageProvider())
                .description(f.getDescription())
                .expenseId(f.getExpense() != null ? f.getExpense().getId() : null)
                .expenseDescription(f.getExpense() != null ? f.getExpense().getDescription() : null)
                .createdAt(f.getCreatedAt()).build();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
