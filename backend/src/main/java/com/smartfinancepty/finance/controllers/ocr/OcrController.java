package com.smartfinancepty.finance.controllers.ocr;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.dto.ExpenseResponse;
import com.smartfinancepty.finance.dto.ocr.OcrConfirmRequest;
import com.smartfinancepty.finance.dto.ocr.OcrResult;
import com.smartfinancepty.finance.infrastructure.kafka.event.OcrRequestedEvent;
import com.smartfinancepty.finance.infrastructure.kafka.producer.FinanceEventProducer;
import com.smartfinancepty.finance.service.ocr.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR", description = "Extracción de datos de facturas y recibos con Tesseract")
@SecurityRequirement(name = "bearerAuth")
public class OcrController {

    private final OcrService ocrService;
    private final FinanceEventProducer eventProducer;

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Escanear factura en tiempo real",
            description = "Sube una imagen o PDF y extrae datos automáticamente (monto, fecha, comercio, items)")
    public ResponseEntity<OcrResult> scanFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "autoCreate", defaultValue = "false") boolean autoCreate,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "expenseId", required = false) Long expenseId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ocrService.processUploadedFile(file, autoCreate, categoryId,
                expenseId, user.getId()));
    }

    @PostMapping("/scan-async")
    @Operation(summary = "Escanear factura en background (Kafka)",
            description = "Publica el OCR a Kafka y retorna inmediatamente. "
                    + "El resultado llega via notificación push.")
    public ResponseEntity<Map<String, String>> scanAsync(@RequestParam Long fileId,
            @RequestParam(defaultValue = "false") boolean autoCreate,
            @RequestParam(required = false) Long categoryId, @AuthenticationPrincipal User user) {

        eventProducer.publishOcrRequested(OcrRequestedEvent.builder().userId(user.getId())
                .fileAttachmentId(fileId).autoCreate(autoCreate).categoryId(categoryId).build());

        return ResponseEntity.accepted()
                .body(Map.of("message",
                        "OCR en proceso. Recibirás una notificación cuando esté listo.", "fileId",
                        fileId.toString()));
    }

    @PostMapping("/process/{fileId}")
    @Operation(summary = "Procesar archivo ya subido",
            description = "Ejecuta OCR sobre un archivo que ya fue subido previamente")
    public ResponseEntity<OcrResult> processExistingFile(@PathVariable Long fileId,
            @RequestParam(value = "autoCreate", defaultValue = "false") boolean autoCreate,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity
                .ok(ocrService.processFileAttachment(fileId, user.getId(), autoCreate, categoryId));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirmar sugerencia y crear gasto",
            description = "El usuario revisa los datos del OCR y confirma la creación del gasto")
    public ResponseEntity<ExpenseResponse> confirmExpense(@RequestBody OcrConfirmRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ocrService.confirmAndCreateExpense(request.getOcrResult(),
                user.getId(), request.getCategoryId(), request.getFileId()));
    }
}
