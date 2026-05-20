package com.smartfinancepty.finance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import com.smartfinancepty.finance.domain.Expense;
import com.smartfinancepty.finance.domain.ExpenseCategory;
import com.smartfinancepty.finance.domain.ExpenseType;
import com.smartfinancepty.finance.domain.Role;
import com.smartfinancepty.finance.domain.User;
import com.smartfinancepty.finance.domain.attachment.FileAttachment;
import com.smartfinancepty.finance.dto.attachment.FileAttachmentResponse;
import com.smartfinancepty.finance.exception.ResourceNotFoundException;
import com.smartfinancepty.finance.repository.ExpenseRepository;
import com.smartfinancepty.finance.repository.UserRepository;
import com.smartfinancepty.finance.repository.attachment.FileAttachmentRepository;
import com.smartfinancepty.finance.service.storage.StorageResult;
import com.smartfinancepty.finance.service.storage.StorageService;
import com.smartfinancepty.finance.service.upload.FileAttachmentService;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileAttachmentService Tests")
class FileAttachmentServiceTest {

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private FileAttachmentService fileAttachmentService;

    private User testUser;
    private ExpenseCategory testCategory;
    private Expense testExpense;
    private FileAttachment testAttachment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileAttachmentService, "maxSizeMb", 10L);

        testUser = User.builder().id(1L).email("joel@smartfinance.com").role(Role.USER).build();

        testCategory = ExpenseCategory.builder().id(1L).name("Alimentación").build();

        testExpense = Expense.builder().id(1L).user(testUser).category(testCategory)
                .description("Compra Super 99").amount(new BigDecimal("85.00"))
                .expenseType(ExpenseType.VARIABLE).expenseDate(LocalDate.now()).active(true).build();

        testAttachment = FileAttachment.builder().id(1L).user(testUser).expense(testExpense)
                .originalFilename("recibo.jpg").storedFilename("uuid-recibo.jpg")
                .contentType("image/jpeg").fileSize(512000L).storageProvider("LOCAL")
                .fileUrl("http://localhost/uploads/uuid-recibo.jpg")
                .storageKey("receipts/1/uuid-recibo.jpg").description("Recibo de compra")
                .active(true).build();
    }

    @Nested
    @DisplayName("Upload File")
    class UploadFileTests {

        @Test
        @DisplayName("Debe subir archivo y asociarlo a gasto")
        void shouldUploadFileAndLinkToExpense() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "recibo.jpg", "image/jpeg", new byte[512000]);

            StorageResult storageResult = StorageResult.builder()
                    .fileUrl("http://localhost/uploads/uuid-recibo.jpg")
                    .storageKey("receipts/1/uuid-recibo.jpg").storedFilename("uuid-recibo.jpg")
                    .provider("LOCAL").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testExpense));
            when(storageService.upload(any(), any())).thenReturn(storageResult);
            when(fileAttachmentRepository.save(any())).thenReturn(testAttachment);

            FileAttachmentResponse result = fileAttachmentService.uploadFile(file, 1L, "Recibo", 1L);

            assertThat(result).isNotNull();
            assertThat(result.getOriginalFilename()).isEqualTo("recibo.jpg");
            assertThat(result.getExpenseId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Debe subir archivo sin asociarlo a gasto")
        void shouldUploadFileWithoutExpense() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", new byte[100]);

            FileAttachment attachmentNoExpense = FileAttachment.builder().id(2L).user(testUser)
                    .originalFilename("doc.pdf").storedFilename("uuid-doc.pdf")
                    .contentType("application/pdf").fileSize(100L).storageProvider("LOCAL")
                    .fileUrl("http://localhost/uploads/uuid-doc.pdf").active(true).build();

            StorageResult storageResult = StorageResult.builder()
                    .fileUrl("http://localhost/uploads/uuid-doc.pdf")
                    .storedFilename("uuid-doc.pdf").provider("LOCAL").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(storageService.upload(any(), any())).thenReturn(storageResult);
            when(fileAttachmentRepository.save(any())).thenReturn(attachmentNoExpense);

            FileAttachmentResponse result = fileAttachmentService.uploadFile(file, null, null, 1L);

            assertThat(result.getExpenseId()).isNull();
        }

        @Test
        @DisplayName("Debe lanzar excepción si el archivo está vacío")
        void shouldThrowWhenFileIsEmpty() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(emptyFile, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede estar vacío");
        }

        @Test
        @DisplayName("Debe lanzar excepción si el archivo supera el tamaño máximo")
        void shouldThrowWhenFileTooLarge() {
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file", "large.jpg", "image/jpeg", new byte[11 * 1024 * 1024]);

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(largeFile, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tamaño máximo");
        }

        @Test
        @DisplayName("Debe lanzar excepción si el tipo de archivo no está permitido")
        void shouldThrowWhenContentTypeNotAllowed() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "video.mp4", "video/mp4", new byte[1000]);

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(file, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tipo de archivo no permitido");
        }

        @Test
        @DisplayName("Debe lanzar excepción si el tipo de archivo es null")
        void shouldThrowWhenContentTypeIsNull() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "file.bin", null, new byte[1000]);

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(file, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Debe lanzar excepción si usuario no existe")
        void shouldThrowWhenUserNotFound() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", new byte[100]);

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(file, null, null, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Debe lanzar excepción si gasto no existe")
        void shouldThrowWhenExpenseNotFound() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", new byte[100]);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(expenseRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(file, 99L, null, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Files")
    class GetFilesTests {

        @Test
        @DisplayName("Debe retornar todos los archivos del usuario")
        void shouldReturnAllFiles() {
            when(fileAttachmentRepository.findByUserIdAndActiveTrue(1L))
                    .thenReturn(List.of(testAttachment));

            List<FileAttachmentResponse> result = fileAttachmentService.getAllFiles(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginalFilename()).isEqualTo("recibo.jpg");
        }

        @Test
        @DisplayName("Debe retornar archivos por gasto")
        void shouldReturnFilesByExpense() {
            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testExpense));
            when(fileAttachmentRepository.findByExpenseIdAndActiveTrue(1L))
                    .thenReturn(List.of(testAttachment));

            List<FileAttachmentResponse> result = fileAttachmentService.getFilesByExpense(1L, 1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe lanzar excepción si gasto no existe al buscar archivos por gasto")
        void shouldThrowWhenExpenseNotFoundForFilesByExpense() {
            when(expenseRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileAttachmentService.getFilesByExpense(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Debe retornar archivos sin gasto asociado")
        void shouldReturnUnlinkedFiles() {
            when(fileAttachmentRepository.findByUserIdAndExpenseIsNullAndActiveTrue(1L))
                    .thenReturn(List.of(testAttachment));

            List<FileAttachmentResponse> result = fileAttachmentService.getUnlinkedFiles(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar archivo por ID")
        void shouldReturnFileById() {
            when(fileAttachmentRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testAttachment));

            FileAttachmentResponse result = fileAttachmentService.getFileById(1L, 1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Debe lanzar excepción si archivo no existe")
        void shouldThrowWhenFileNotFound() {
            when(fileAttachmentRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileAttachmentService.getFileById(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Archivo adjunto no encontrado");
        }
    }

    @Nested
    @DisplayName("Link To Expense")
    class LinkToExpenseTests {

        @Test
        @DisplayName("Debe asociar archivo a un gasto")
        void shouldLinkFileToExpense() {
            FileAttachment unlinked = FileAttachment.builder().id(2L).user(testUser)
                    .originalFilename("doc.pdf").storedFilename("uuid.pdf")
                    .contentType("application/pdf").fileSize(100L).storageProvider("LOCAL")
                    .fileUrl("http://localhost/doc.pdf").active(true).build();

            when(fileAttachmentRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(unlinked));
            when(expenseRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testExpense));
            when(fileAttachmentRepository.save(any())).thenReturn(testAttachment);

            FileAttachmentResponse result = fileAttachmentService.linkToExpense(2L, 1L, 1L);

            assertThat(result.getExpenseId()).isEqualTo(1L);
            verify(fileAttachmentRepository).save(unlinked);
        }

        @Test
        @DisplayName("Debe lanzar excepción si archivo no existe al asociar")
        void shouldThrowWhenFileNotFoundOnLink() {
            when(fileAttachmentRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileAttachmentService.linkToExpense(99L, 1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete File")
    class DeleteFileTests {

        @Test
        @DisplayName("Debe eliminar archivo del storage y hacer soft delete en DB")
        void shouldDeleteFile() {
            when(fileAttachmentRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testAttachment));
            when(fileAttachmentRepository.save(any())).thenReturn(testAttachment);

            fileAttachmentService.deleteFile(1L, 1L);

            assertThat(testAttachment.isActive()).isFalse();
            verify(storageService).delete(testAttachment.getStorageKey());
        }

        @Test
        @DisplayName("Debe lanzar excepción al eliminar archivo inexistente")
        void shouldThrowWhenDeletingNonExistentFile() {
            when(fileAttachmentRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileAttachmentService.deleteFile(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Format File Size")
    class FormatFileSizeTests {

        @Test
        @DisplayName("Debe formatear tamaño en bytes")
        void shouldFormatInBytes() {
            FileAttachment smallFile = FileAttachment.builder().id(3L).user(testUser)
                    .originalFilename("tiny.jpg").storedFilename("uuid.jpg")
                    .contentType("image/jpeg").fileSize(512L).storageProvider("LOCAL")
                    .fileUrl("http://localhost/tiny.jpg").active(true).build();

            when(fileAttachmentRepository.findByUserIdAndActiveTrue(1L))
                    .thenReturn(List.of(smallFile));

            List<FileAttachmentResponse> result = fileAttachmentService.getAllFiles(1L);

            assertThat(result.get(0).getFileSizeFormatted()).contains("B");
        }

        @Test
        @DisplayName("Debe formatear tamaño en KB")
        void shouldFormatInKb() {
            FileAttachment kbFile = FileAttachment.builder().id(4L).user(testUser)
                    .originalFilename("medium.jpg").storedFilename("uuid2.jpg")
                    .contentType("image/jpeg").fileSize(5120L).storageProvider("LOCAL")
                    .fileUrl("http://localhost/medium.jpg").active(true).build();

            when(fileAttachmentRepository.findByUserIdAndActiveTrue(1L))
                    .thenReturn(List.of(kbFile));

            List<FileAttachmentResponse> result = fileAttachmentService.getAllFiles(1L);

            assertThat(result.get(0).getFileSizeFormatted()).contains("KB");
        }

        @Test
        @DisplayName("Debe formatear tamaño en MB")
        void shouldFormatInMb() {
            FileAttachment mbFile = FileAttachment.builder().id(5L).user(testUser)
                    .originalFilename("large.jpg").storedFilename("uuid3.jpg")
                    .contentType("image/jpeg").fileSize(2 * 1024 * 1024L).storageProvider("LOCAL")
                    .fileUrl("http://localhost/large.jpg").active(true).build();

            when(fileAttachmentRepository.findByUserIdAndActiveTrue(1L))
                    .thenReturn(List.of(mbFile));

            List<FileAttachmentResponse> result = fileAttachmentService.getAllFiles(1L);

            assertThat(result.get(0).getFileSizeFormatted()).contains("MB");
        }
    }
}
