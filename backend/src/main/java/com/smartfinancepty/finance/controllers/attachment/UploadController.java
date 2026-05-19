package com.smartfinancepty.finance.controllers.attachment;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.attachment.FileAttachmentResponse;
import com.smartfinancepty.finance.service.upload.FileAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Upload y gestión de archivos adjuntos")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private final FileAttachmentService fileAttachmentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir archivo (recibo, factura, PDF)")
    public ResponseEntity<FileAttachmentResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long expenseId,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileAttachmentService.uploadFile(file, expenseId, description, user.getId()));
    }

    @GetMapping
    @Operation(summary = "Obtener todos los archivos del usuario")
    public ResponseEntity<List<FileAttachmentResponse>> getAllFiles(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileAttachmentService.getAllFiles(user.getId()));
    }

    @GetMapping("/unlinked")
    @Operation(summary = "Archivos sin gasto asociado")
    public ResponseEntity<List<FileAttachmentResponse>> getUnlinkedFiles(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileAttachmentService.getUnlinkedFiles(user.getId()));
    }

    @GetMapping("/expense/{expenseId}")
    @Operation(summary = "Archivos de un gasto específico")
    public ResponseEntity<List<FileAttachmentResponse>> getFilesByExpense(
            @PathVariable Long expenseId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileAttachmentService.getFilesByExpense(expenseId, user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener archivo por ID")
    public ResponseEntity<FileAttachmentResponse> getFileById(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileAttachmentService.getFileById(id, user.getId()));
    }

    @PatchMapping("/{fileId}/link/{expenseId}")
    @Operation(summary = "Asociar archivo existente a un gasto")
    public ResponseEntity<FileAttachmentResponse> linkToExpense(@PathVariable Long fileId,
            @PathVariable Long expenseId, @AuthenticationPrincipal User user) {
        return ResponseEntity
                .ok(fileAttachmentService.linkToExpense(fileId, expenseId, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar archivo")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        fileAttachmentService.deleteFile(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
