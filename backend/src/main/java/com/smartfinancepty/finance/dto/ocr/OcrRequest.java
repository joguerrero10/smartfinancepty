package com.smartfinancepty.finance.dto.ocr;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrRequest {
    private Long fileAttachmentId; // procesar archivo ya subido
    private boolean autoCreateExpense; // crear gasto automáticamente
    private Long categoryId; // categoría para el gasto (opcional)
}
