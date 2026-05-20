package com.smartfinancepty.finance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import com.smartfinancepty.finance.domain.*;
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
import com.smartfinancepty.finance.service.ocr.OcrService;

@ExtendWith(MockitoExtension.class)
@DisplayName("OcrService Tests")
class OcrServiceTest {

    @Mock
    private TesseractOcrEngine tesseractEngine;
    @Mock
    private InvoiceParser invoiceParser;
    @Mock
    private FileAttachmentRepository fileAttachmentRepository;
    @Mock
    private ExpenseCategoryRepository categoryRepository;
    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private OcrService ocrService;

    private User testUser;
    private ExpenseCategory testCategory;
    private FileAttachment testAttachment;
    private OcrResult successResult;
    private ExpenseResponse expenseResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ocrService, "uploadPath", "uploads");

        testUser = User.builder().id(1L).email("joel@smartfinance.com").role(Role.USER).build();

        testCategory = ExpenseCategory.builder().id(1L).name("Alimentación").build();

        testAttachment = FileAttachment.builder().id(1L).user(testUser)
                .originalFilename("factura.jpg").storedFilename("uuid-factura.jpg")
                .contentType("image/jpeg").fileSize(100000L).storageProvider("LOCAL")
                .fileUrl("http://localhost/uploads/receipts/1/uuid-factura.jpg")
                .storageKey("receipts/1/uuid-factura.jpg").active(true).build();

        successResult = OcrResult.builder().rawText("Super Rey\nTotal: $45.00")
                .totalAmount(new BigDecimal("45.00")).invoiceDate(LocalDate.now())
                .merchantName("Super Rey").confidence(0.9).processed(true)
                .lineItems(java.util.List.of()).build();

        expenseResponse = ExpenseResponse.builder().id(10L).description("Compra en Super Rey")
                .amount(new BigDecimal("45.00")).expenseType(ExpenseType.VARIABLE)
                .categoryName("Alimentación").expenseDate(LocalDate.now()).active(true).build();
    }

    @Nested
    @DisplayName("processFileAttachment")
    class ProcessFileAttachmentTests {

        @Test
        @DisplayName("Debe procesar archivo adjunto exitosamente")
        void shouldProcessFileAttachmentSuccessfully() throws Exception {
            when(fileAttachmentRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testAttachment));
            when(tesseractEngine.extractTextFromFile(any())).thenReturn("Super Rey\nTotal: $45.00");
            when(invoiceParser.parse(any())).thenReturn(successResult);

            OcrResult result = ocrService.processFileAttachment(1L, 1L, false, null);

            assertThat(result.isProcessed()).isTrue();
            assertThat(result.getTotalAmount()).isEqualByComparingTo("45.00");
        }

        @Test
        @DisplayName("Debe crear gasto automáticamente si autoCreate=true y hay monto")
        void shouldAutoCreateExpenseWhenRequested() throws Exception {
            OcrResult resultWithSuggestion = OcrResult.builder().rawText("Super Rey\nTotal: $45.00")
                    .totalAmount(new BigDecimal("45.00")).invoiceDate(LocalDate.now())
                    .merchantName("Super Rey").confidence(0.9).processed(true)
                    .lineItems(java.util.List.of())
                    .suggestion(OcrResult.ExpenseSuggestion.builder()
                            .suggestedAmount(new BigDecimal("45.00")).suggestedCategoryId(2L)
                            .suggestedDescription("Compra en Super Rey")
                            .suggestedDate(LocalDate.now()).confidence("HIGH").build())
                    .build();

            when(fileAttachmentRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testAttachment));
            when(tesseractEngine.extractTextFromFile(any())).thenReturn("Super Rey\nTotal: $45.00");
            when(invoiceParser.parse(any())).thenReturn(resultWithSuggestion);
            when(expenseService.createExpense(any(ExpenseRequest.class), eq(1L)))
                    .thenReturn(expenseResponse);
            when(fileAttachmentRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testAttachment));
            when(expenseService.getExpenseEntity(eq(10L), eq(1L))).thenReturn(
                    Expense.builder().id(10L).user(testUser).category(testCategory)
                            .description("Compra").amount(new BigDecimal("45.00"))
                            .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now()).build());
            when(fileAttachmentRepository.save(any())).thenReturn(testAttachment);

            ocrService.processFileAttachment(1L, 1L, true, 2L);

            verify(expenseService).createExpense(any(ExpenseRequest.class), eq(1L));
        }

        @Test
        @DisplayName("Debe lanzar excepción si archivo no existe")
        void shouldThrowWhenFileNotFound() {
            when(fileAttachmentRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ocrService.processFileAttachment(99L, 1L, false, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Archivo no encontrado");
        }

        @Test
        @DisplayName("Debe lanzar excepción si tipo de archivo no es imagen")
        void shouldThrowWhenContentTypeNotSupported() {
            FileAttachment pptAttachment = FileAttachment.builder().id(2L).user(testUser)
                    .originalFilename("presentacion.pptx").storedFilename("uuid.pptx")
                    .contentType("application/vnd.ms-powerpoint").fileSize(5000L)
                    .storageProvider("LOCAL").fileUrl("http://localhost/pptx")
                    .storageKey("receipts/1/uuid.pptx").active(true).build();

            when(fileAttachmentRepository.findByIdAndUserId(2L, 1L))
                    .thenReturn(Optional.of(pptAttachment));

            assertThatThrownBy(() -> ocrService.processFileAttachment(2L, 1L, false, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OCR solo soporta");
        }

        @Test
        @DisplayName("Debe retornar resultado con error cuando OCR falla")
        void shouldReturnErrorResultWhenOcrFails() throws Exception {
            when(fileAttachmentRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testAttachment));
            when(tesseractEngine.extractTextFromFile(any()))
                    .thenThrow(new RuntimeException("Tesseract error"));

            OcrResult result = ocrService.processFileAttachment(1L, 1L, false, null);

            assertThat(result.isProcessed()).isFalse();
            assertThat(result.getErrorMessage()).contains("Error al procesar");
        }

        @Test
        @DisplayName("Debe lanzar excepción si contentType es null")
        void shouldThrowWhenContentTypeIsNull() {
            FileAttachment nullContentAttachment = FileAttachment.builder().id(3L).user(testUser)
                    .originalFilename("file").storedFilename("uuid")
                    .contentType(null).fileSize(1000L).storageProvider("LOCAL")
                    .fileUrl("http://localhost/file").storageKey("key").active(true).build();

            when(fileAttachmentRepository.findByIdAndUserId(3L, 1L))
                    .thenReturn(Optional.of(nullContentAttachment));

            assertThatThrownBy(() -> ocrService.processFileAttachment(3L, 1L, false, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("desconocido");
        }
    }

    @Nested
    @DisplayName("processUploadedFile")
    class ProcessUploadedFileTests {

        @Test
        @DisplayName("Debe procesar archivo subido en tiempo real")
        void shouldProcessUploadedFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "factura.jpg", "image/jpeg", "fake-image-data".getBytes());

            when(tesseractEngine.extractText(any())).thenReturn("Super 99\nTotal: $30.00");
            when(invoiceParser.parse(any())).thenReturn(successResult);

            OcrResult result = ocrService.processUploadedFile(file, false, null, null, 1L);

            assertThat(result.isProcessed()).isTrue();
            verify(tesseractEngine).extractText(file);
        }

        @Test
        @DisplayName("Debe lanzar excepción si tipo no soportado en upload")
        void shouldThrowWhenContentTypeNotSupportedInUpload() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "video.mp4", "video/mp4", "fake".getBytes());

            assertThatThrownBy(() -> ocrService.processUploadedFile(file, false, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Debe crear gasto automáticamente al subir si autoCreate=true")
        void shouldAutoCreateExpenseOnUpload() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "factura.jpg", "image/jpeg", "fake-data".getBytes());

            OcrResult resultWithSuggestion = OcrResult.builder().rawText("Super Rey\nTotal: $45.00")
                    .totalAmount(new BigDecimal("45.00")).invoiceDate(LocalDate.now())
                    .merchantName("Super Rey").confidence(0.9).processed(true)
                    .lineItems(java.util.List.of())
                    .suggestion(OcrResult.ExpenseSuggestion.builder()
                            .suggestedAmount(new BigDecimal("45.00")).suggestedCategoryId(2L)
                            .suggestedDescription("Compra en Super Rey")
                            .suggestedDate(LocalDate.now()).confidence("HIGH").build())
                    .build();

            when(tesseractEngine.extractText(any())).thenReturn("Super Rey\nTotal: $45.00");
            when(invoiceParser.parse(any())).thenReturn(resultWithSuggestion);
            when(expenseService.createExpense(any(), eq(1L))).thenReturn(expenseResponse);

            ocrService.processUploadedFile(file, true, 2L, null, 1L);

            verify(expenseService).createExpense(any(ExpenseRequest.class), eq(1L));
        }

        @Test
        @DisplayName("Debe retornar error cuando OCR falla en upload")
        void shouldReturnErrorWhenOcrFailsOnUpload() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "factura.png", "image/png", "fake".getBytes());

            when(tesseractEngine.extractText(any())).thenThrow(new RuntimeException("OCR error"));

            OcrResult result = ocrService.processUploadedFile(file, false, null, null, 1L);

            assertThat(result.isProcessed()).isFalse();
            assertThat(result.getErrorMessage()).contains("Error al procesar");
        }
    }

    @Nested
    @DisplayName("confirmAndCreateExpense")
    class ConfirmAndCreateExpenseTests {

        @Test
        @DisplayName("Debe crear gasto al confirmar resultado OCR")
        void shouldCreateExpenseOnConfirm() {
            OcrResult result = OcrResult.builder().totalAmount(new BigDecimal("45.00"))
                    .suggestion(OcrResult.ExpenseSuggestion.builder()
                            .suggestedAmount(new BigDecimal("45.00")).suggestedCategoryId(1L)
                            .suggestedDescription("Compra en Super Rey")
                            .suggestedDate(LocalDate.now()).confidence("HIGH").build())
                    .build();

            when(expenseService.createExpense(any(), eq(1L))).thenReturn(expenseResponse);

            ExpenseResponse response = ocrService.confirmAndCreateExpense(result, 1L, 1L, null);

            assertThat(response.getId()).isEqualTo(10L);
            verify(expenseService).createExpense(any(ExpenseRequest.class), eq(1L));
        }

        @Test
        @DisplayName("Debe lanzar excepción si monto es null al confirmar")
        void shouldThrowWhenTotalAmountIsNullOnConfirm() {
            OcrResult result = OcrResult.builder().totalAmount(null).build();

            assertThatThrownBy(() -> ocrService.confirmAndCreateExpense(result, 1L, 1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto válido");
        }

        @Test
        @DisplayName("Debe lanzar excepción si monto es cero al confirmar")
        void shouldThrowWhenTotalAmountIsZeroOnConfirm() {
            OcrResult result = OcrResult.builder().totalAmount(BigDecimal.ZERO).build();

            assertThatThrownBy(() -> ocrService.confirmAndCreateExpense(result, 1L, 1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto válido");
        }

        @Test
        @DisplayName("Debe asociar archivo al gasto creado si fileId no es null")
        void shouldLinkFileToExpenseOnConfirm() {
            OcrResult result = OcrResult.builder().totalAmount(new BigDecimal("45.00"))
                    .suggestion(OcrResult.ExpenseSuggestion.builder()
                            .suggestedAmount(new BigDecimal("45.00")).suggestedCategoryId(1L)
                            .suggestedDescription("Compra").suggestedDate(LocalDate.now())
                            .confidence("HIGH").build())
                    .build();

            Expense expense = Expense.builder().id(10L).user(testUser).category(testCategory)
                    .description("Compra").amount(new BigDecimal("45.00"))
                    .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now()).build();

            when(expenseService.createExpense(any(), eq(1L))).thenReturn(expenseResponse);
            when(fileAttachmentRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testAttachment));
            when(expenseService.getExpenseEntity(eq(10L), eq(1L))).thenReturn(expense);
            when(fileAttachmentRepository.save(any())).thenReturn(testAttachment);

            ocrService.confirmAndCreateExpense(result, 1L, 1L, 1L);

            verify(fileAttachmentRepository).save(testAttachment);
        }
    }

    @Nested
    @DisplayName("buildSuggestion — auto-detección de categoría")
    class BuildSuggestionTests {

        @Test
        @DisplayName("Debe auto-detectar categoría Alimentación para 'Super'")
        void shouldAutoDetectFoodCategoryForSupermarket() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "factura.jpg", "image/jpeg", "fake".getBytes());

            OcrResult resultWithMerchant = OcrResult.builder().totalAmount(new BigDecimal("50.00"))
                    .invoiceDate(LocalDate.now()).merchantName("Super 99")
                    .confidence(0.9).processed(true).lineItems(java.util.List.of()).build();

            when(tesseractEngine.extractText(any())).thenReturn("Super 99\nTotal: $50.00");
            when(invoiceParser.parse(any())).thenReturn(resultWithMerchant);
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(testCategory));

            OcrResult result = ocrService.processUploadedFile(file, false, null, null, 1L);

            assertThat(result.getSuggestion()).isNotNull();
            assertThat(result.getSuggestion().getSuggestedCategoryId()).isNotNull();
        }

        @Test
        @DisplayName("Debe usar categoría por defecto (9L) cuando no se detecta comercio")
        void shouldUseDefaultCategoryWhenNoMerchantDetected() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "factura.jpg", "image/jpeg", "fake".getBytes());

            OcrResult resultNoMerchant = OcrResult.builder().totalAmount(new BigDecimal("30.00"))
                    .invoiceDate(LocalDate.now()).merchantName(null)
                    .confidence(0.7).processed(true).lineItems(java.util.List.of()).build();

            when(tesseractEngine.extractText(any())).thenReturn("texto sin comercio");
            when(invoiceParser.parse(any())).thenReturn(resultNoMerchant);

            OcrResult result = ocrService.processUploadedFile(file, false, null, null, 1L);

            assertThat(result.getSuggestion().getSuggestedCategoryId()).isEqualTo(9L);
        }

        @Test
        @DisplayName("Debe usar confidence LOW cuando total es null")
        void shouldUseLowConfidenceWhenTotalIsNull() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "factura.png", "image/png", "fake".getBytes());

            OcrResult noAmountResult = OcrResult.builder().totalAmount(null)
                    .merchantName("Comercio ABC").confidence(0.3).processed(true)
                    .lineItems(java.util.List.of()).build();

            when(tesseractEngine.extractText(any())).thenReturn("texto");
            when(invoiceParser.parse(any())).thenReturn(noAmountResult);

            OcrResult result = ocrService.processUploadedFile(file, false, null, null, 1L);

            assertThat(result.getSuggestion().getConfidence()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("Confidence HIGH cuando result.confidence >= 0.8")
        void shouldReturnHighConfidenceWhenResultConfidenceIsHigh() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "factura.jpg", "image/jpeg", "fake".getBytes());

            OcrResult highConfidenceResult = OcrResult.builder().totalAmount(new BigDecimal("50.00"))
                    .merchantName("Restaurante Bella Italia").confidence(0.85)
                    .processed(true).lineItems(java.util.List.of()).build();

            when(tesseractEngine.extractText(any())).thenReturn("texto");
            when(invoiceParser.parse(any())).thenReturn(highConfidenceResult);

            OcrResult result = ocrService.processUploadedFile(file, false, null, null, 1L);

            assertThat(result.getSuggestion().getConfidence()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Confidence MEDIUM cuando result.confidence entre 0.5 y 0.8")
        void shouldReturnMediumConfidenceWhenBetween50And80() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "factura.jpg", "image/jpeg", "fake".getBytes());

            OcrResult mediumConfidenceResult = OcrResult.builder()
                    .totalAmount(new BigDecimal("30.00")).merchantName("Tienda X")
                    .confidence(0.65).processed(true).lineItems(java.util.List.of()).build();

            when(tesseractEngine.extractText(any())).thenReturn("texto");
            when(invoiceParser.parse(any())).thenReturn(mediumConfidenceResult);

            OcrResult result = ocrService.processUploadedFile(file, false, null, null, 1L);

            assertThat(result.getSuggestion().getConfidence()).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Debe usar descripción genérica cuando merchant es null")
        void shouldUseGenericDescriptionWhenMerchantIsNull() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "factura.jpg", "image/jpeg", "fake".getBytes());

            OcrResult noMerchantResult = OcrResult.builder().totalAmount(new BigDecimal("50.00"))
                    .merchantName(null).confidence(0.5).processed(true)
                    .lineItems(java.util.List.of()).build();

            when(tesseractEngine.extractText(any())).thenReturn("texto");
            when(invoiceParser.parse(any())).thenReturn(noMerchantResult);

            OcrResult result = ocrService.processUploadedFile(file, false, null, null, 1L);

            assertThat(result.getSuggestion().getSuggestedDescription())
                    .isEqualTo("Gasto detectado por OCR");
        }
    }
}
