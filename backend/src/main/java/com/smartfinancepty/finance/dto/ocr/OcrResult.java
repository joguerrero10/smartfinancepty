package com.smartfinancepty.finance.dto.ocr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrResult {

    // ── Datos extraídos del OCR ───────────────────────────────────────────────
    private String rawText; // texto completo extraído
    private BigDecimal totalAmount; // monto total de la factura
    private LocalDate invoiceDate; // fecha de la factura
    private String merchantName; // nombre del comercio
    private String merchantRuc; // RUC del comercio (Panamá)
    private List<OcrLineItem> lineItems; // items individuales
    private String currency; // PAB, USD
    private BigDecimal taxAmount; // ITBMS (7% en Panamá)
    private BigDecimal subtotal; // subtotal antes de impuesto

    // ── Metadata del proceso ──────────────────────────────────────────────────
    private double confidence; // 0.0 - 1.0
    private String language; // es, en
    private boolean processed;
    private String errorMessage;

    // ── Sugerencia de gasto ───────────────────────────────────────────────────
    private ExpenseSuggestion suggestion;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OcrLineItem {
        private String description;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpenseSuggestion {
        private BigDecimal suggestedAmount;
        private Long suggestedCategoryId;
        private String suggestedCategoryName;
        private String suggestedDescription;
        private LocalDate suggestedDate;
        private String confidence; // HIGH, MEDIUM, LOW
    }
}
