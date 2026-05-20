package com.smartfinancepty.finance.dto.ocr;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrConfirmRequest {
    private OcrResult ocrResult;
    private Long categoryId;
    private Long fileId;
}
