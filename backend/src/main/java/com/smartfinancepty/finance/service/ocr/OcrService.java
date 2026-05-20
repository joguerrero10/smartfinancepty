package com.smartfinancepty.finance.service.ocr;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.smartfinancepty.finance.domain.ExpenseCategory;
import com.smartfinancepty.finance.domain.ExpenseType;
import com.smartfinancepty.finance.domain.attachment.FileAttachment;
import com.smartfinancepty.finance.dto.ExpenseRequest;
import com.smartfinancepty.finance.dto.ExpenseResponse;
import com.smartfinancepty.finance.dto.ocr.OcrResult;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.infrastructure.ocr.InvoiceParser;
import com.smartfinancepty.finance.infrastructure.ocr.TesseractOcrEngine;
import com.smartfinancepty.finance.repository.ExpenseCategoryRepository;
import com.smartfinancepty.finance.repository.attachment.FileAttachmentRepository;
import com.smartfinancepty.finance.service.finance.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {

    private final TesseractOcrEngine tesseractEngine;
    private final InvoiceParser invoiceParser;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseService expenseService;

    @Value("${app.storage.local.path:uploads}")
    private String uploadPath;

    // Mapeo de palabras clave a categorías
    private static final Map<String, Long> MERCHANT_CATEGORY_MAP = Map.of("super", 2L, // Supermercado
                                                                                       // →
                                                                                       // Alimentación
            "rey", 2L, // Super Rey → Alimentación
            "riba smith", 2L, // → Alimentación
            "farmacia", 4L, // → Salud
            "gas", 3L, // → Transporte
            "gasolinera", 3L, // → Transporte
            "shell", 3L, // → Transporte
            "cine", 5L, // → Entretenimiento
            "restaurante", 2L, // → Alimentación
            "soda", 2L);

    // ── OCR desde archivo subido ──────────────────────────────────────────────

    @Transactional
    public OcrResult processFileAttachment(Long fileId, Long userId, boolean autoCreate,
            Long categoryId) {
        FileAttachment attachment = fileAttachmentRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));

        validateImageFile(attachment.getContentType());

        try {
            // Construir ruta del archivo local
            String storageKey = attachment.getStorageKey();
            Path filePath = Paths.get(uploadPath, storageKey);

            log.info("🔍 Iniciando OCR en: {}", filePath);
            String rawText = tesseractEngine.extractTextFromFile(filePath.toFile());
            OcrResult result = invoiceParser.parse(rawText);

            // Agregar sugerencia de gasto
            result.setSuggestion(buildSuggestion(result, categoryId));

            // Crear gasto automáticamente si se solicitó
            if (autoCreate && result.getTotalAmount() != null) {
                ExpenseResponse expense = autoCreateExpense(result, fileId, userId, categoryId);
                log.info("✅ Gasto creado automáticamente: ID={}, monto={}", expense.getId(),
                        expense.getAmount());
            }

            return result;

        } catch (Exception e) {
            log.error("Error en OCR para archivo {}: {}", fileId, e.getMessage());
            return OcrResult.builder().processed(false)
                    .errorMessage("Error al procesar el archivo: " + e.getMessage()).build();
        }
    }

    // ── OCR desde archivo subido en tiempo real ───────────────────────────────

    @Transactional
    public OcrResult processUploadedFile(MultipartFile file, boolean autoCreate, Long categoryId,
            Long expenseId, Long userId) {
        validateImageFile(file.getContentType());

        try {
            log.info("🔍 OCR en tiempo real: {}", file.getOriginalFilename());
            String rawText = tesseractEngine.extractText(file);
            OcrResult result = invoiceParser.parse(rawText);
            result.setSuggestion(buildSuggestion(result, categoryId));

            if (autoCreate && result.getTotalAmount() != null) {
                autoCreateExpense(result, expenseId, userId, categoryId);
            }

            return result;

        } catch (Exception e) {
            log.error("Error en OCR upload: {}", e.getMessage());
            return OcrResult.builder().processed(false)
                    .errorMessage("Error al procesar: " + e.getMessage()).build();
        }
    }

    // ── Confirmar sugerencia y crear gasto ────────────────────────────────────

    @Transactional
    public ExpenseResponse confirmAndCreateExpense(OcrResult result, Long userId, Long categoryId,
            Long fileId) {
        if (result.getTotalAmount() == null
                || result.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("No se detectó monto válido en la factura");
        }
        return autoCreateExpense(result, fileId, userId, categoryId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OcrResult.ExpenseSuggestion buildSuggestion(OcrResult result, Long categoryId) {
        if (result.getTotalAmount() == null) {
            return OcrResult.ExpenseSuggestion.builder().confidence("LOW")
                    .suggestedDescription("Factura sin monto detectado").build();
        }

        Long suggestedCatId = categoryId;
        String suggestedCatName = null;

        // Auto-detectar categoría por nombre del comercio
        if (suggestedCatId == null && result.getMerchantName() != null) {
            String merchant = result.getMerchantName().toLowerCase();
            for (Map.Entry<String, Long> entry : MERCHANT_CATEGORY_MAP.entrySet()) {
                if (merchant.contains(entry.getKey())) {
                    suggestedCatId = entry.getValue();
                    break;
                }
            }
        }

        // Obtener nombre de la categoría
        if (suggestedCatId != null) {
            final Long catId = suggestedCatId;
            suggestedCatName =
                    categoryRepository.findById(catId).map(ExpenseCategory::getName).orElse(null);
        }

        if (suggestedCatId == null)
            suggestedCatId = 9L; // Otros por defecto

        String confidence = result.getConfidence() >= 0.8 ? "HIGH"
                : result.getConfidence() >= 0.5 ? "MEDIUM" : "LOW";

        String description =
                result.getMerchantName() != null ? "Compra en " + result.getMerchantName()
                        : "Gasto detectado por OCR";

        return OcrResult.ExpenseSuggestion.builder().suggestedAmount(result.getTotalAmount())
                .suggestedCategoryId(suggestedCatId).suggestedCategoryName(suggestedCatName)
                .suggestedDescription(description)
                .suggestedDate(
                        result.getInvoiceDate() != null ? result.getInvoiceDate() : LocalDate.now())
                .confidence(confidence).build();
    }

    private ExpenseResponse autoCreateExpense(OcrResult result, Long fileId, Long userId,
            Long categoryId) {
        OcrResult.ExpenseSuggestion suggestion = result.getSuggestion();
        Long catId = categoryId != null ? categoryId : suggestion.getSuggestedCategoryId();

        ExpenseRequest request = ExpenseRequest.builder()
                .description(suggestion.getSuggestedDescription()).amount(result.getTotalAmount())
                .expenseType(ExpenseType.VARIABLE).categoryId(catId)
                .expenseDate(suggestion.getSuggestedDate())
                .notes("Creado automáticamente por OCR. Confianza: " + suggestion.getConfidence())
                .build();

        ExpenseResponse expense = expenseService.createExpense(request, userId);

        // Asociar el archivo al gasto creado
        if (fileId != null) {
            fileAttachmentRepository.findByIdAndUserId(fileId, userId).ifPresent(att -> {
                att.setExpense(expenseService.getExpenseEntity(expense.getId(), userId));
                fileAttachmentRepository.save(att);
            });
        }

        return expense;
    }

    private void validateImageFile(String contentType) {
        // Si contentType es null, intentar continuar en vez de rechazar
        if (contentType == null || contentType.equals("application/octet-stream")) {
            return; // Tesseract intentará procesarlo igual
        }

        List<String> supported =
                List.of("image/jpeg", "image/png", "image/webp", "image/tiff", "application/pdf");
        if (!supported.contains(contentType)) {
            throw new IllegalArgumentException(
                    "El OCR solo soporta JPG, PNG, TIFF y PDF. Tipo recibido: " + contentType);
        }
    }
}
