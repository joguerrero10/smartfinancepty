package com.smartfinancepty.finance.infrastructure.ocr;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.smartfinancepty.finance.dto.ocr.OcrResult;
import com.smartfinancepty.finance.dto.ocr.OcrResult.OcrLineItem;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InvoiceParser {

    // ── Patrones para facturas panameñas ──────────────────────────────────────

    // Montos: $123.45, B/.123.45, 123.45, 1,234.56
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:TOTAL|Total|MONTO|Monto|COBRO|PAGAR|IMPORTE)\\s*:?\\s*(?:B/\\.?|\\$)?\\s*(\\d{1,6}(?:[,.]\\d{3})*(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE);

    // Cualquier monto en el texto
    private static final Pattern ANY_AMOUNT =
            Pattern.compile("(?:B/\\.?|\\$)\\s*(\\d{1,6}(?:,\\d{3})*\\.\\d{2})");

    // Fechas: DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD
    private static final Pattern DATE_PATTERN = Pattern
            .compile("(\\d{1,2}[/\\-](\\d{1,2})[/\\-](\\d{2,4}))|(\\d{4}[/\\-]\\d{2}[/\\-]\\d{2})");

    // RUC panameño: ej. 8-123-4567, 8NT-123-4567
    private static final Pattern RUC_PATTERN = Pattern
            .compile("(?:RUC|R\\.U\\.C\\.?)\\s*:?\\s*([\\w\\-]{5,15})", Pattern.CASE_INSENSITIVE);

    // ITBMS (7% en Panamá)
    private static final Pattern ITBMS_PATTERN = Pattern.compile(
            "(?:ITBMS|I\\.T\\.B\\.M\\.S\\.|IMP\\.|TAX|IMPUESTO)\\s*:?\\s*(?:B/\\.?|\\$)?\\s*(\\d{1,4}\\.\\d{2})",
            Pattern.CASE_INSENSITIVE);

    // Líneas de items: descripción .... precio
    private static final Pattern LINE_ITEM_PATTERN =
            Pattern.compile("^(.{3,40})\\s{2,}(\\d{1,4})\\s+(\\d+\\.\\d{2})\\s+(\\d+\\.\\d{2})$",
                    Pattern.MULTILINE);

    // ─────────────────────────────────────────────────────────────────────────

    public OcrResult parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return OcrResult.builder().rawText(rawText).processed(false)
                    .errorMessage("Texto vacío extraído del OCR").build();
        }

        try {
            BigDecimal total = extractTotal(rawText);
            LocalDate date = extractDate(rawText);
            String merchant = extractMerchantName(rawText);
            String ruc = extractRuc(rawText);
            BigDecimal tax = extractItbms(rawText);
            BigDecimal subtotal = calcSubtotal(total, tax);
            List<OcrLineItem> items = extractLineItems(rawText);
            double confidence = calcConfidence(total, date, merchant);

            log.info("📄 Factura parseada: merchant={}, total={}, date={}, items={}", merchant,
                    total, date, items.size());

            return OcrResult.builder().rawText(rawText).totalAmount(total).invoiceDate(date)
                    .merchantName(merchant).merchantRuc(ruc).taxAmount(tax).subtotal(subtotal)
                    .lineItems(items).currency("PAB").confidence(confidence).language("spa")
                    .processed(true).build();

        } catch (Exception e) {
            log.error("Error al parsear factura: {}", e.getMessage());
            return OcrResult.builder().rawText(rawText).processed(false)
                    .errorMessage("Error al procesar: " + e.getMessage()).build();
        }
    }

    // ── Extracción de monto total ─────────────────────────────────────────────

    private BigDecimal extractTotal(String text) {
        // 1. Buscar con palabras clave TOTAL, COBRO, etc.
        Matcher m = AMOUNT_PATTERN.matcher(text);
        BigDecimal lastMatch = null;
        while (m.find()) {
            lastMatch = parseBigDecimal(m.group(1));
        }
        if (lastMatch != null)
            return lastMatch;

        // 2. Buscar el monto más grande en el texto (probablemente el total)
        Matcher anyM = ANY_AMOUNT.matcher(text);
        BigDecimal max = BigDecimal.ZERO;
        while (anyM.find()) {
            BigDecimal val = parseBigDecimal(anyM.group(1));
            if (val != null && val.compareTo(max) > 0)
                max = val;
        }
        return max.compareTo(BigDecimal.ZERO) > 0 ? max : null;
    }

    // ── Extracción de fecha ───────────────────────────────────────────────────

    private LocalDate extractDate(String text) {
        Matcher m = DATE_PATTERN.matcher(text);
        List<DateTimeFormatter> formatters = List.of(DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("dd/MM/yy"));

        while (m.find()) {
            String dateStr = m.group(0);
            for (DateTimeFormatter fmt : formatters) {
                try {
                    return LocalDate.parse(dateStr, fmt);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return LocalDate.now(); // fallback: fecha actual
    }

    // ── Extracción del comercio ───────────────────────────────────────────────

    private String extractMerchantName(String text) {
        String[] lines = text.split("\n");
        // Normalmente el nombre del comercio está en las primeras 3 líneas
        for (int i = 0; i < Math.min(3, lines.length); i++) {
            String line = lines[i].trim();
            if (line.length() > 3 && line.length() < 60 && !line.matches(".*\\d{2}/\\d{2}/\\d{4}.*")
                    && !line.toUpperCase().startsWith("RUC")
                    && !line.toUpperCase().startsWith("DGI") && !line.matches(".*\\$.*")
                    && !line.equalsIgnoreCase("factura") && !line.equalsIgnoreCase("recibo")) {
                return capitalize(line);
            }
        }
        return "Comercio desconocido";
    }

    // ── Extracción RUC ────────────────────────────────────────────────────────

    private String extractRuc(String text) {
        Matcher m = RUC_PATTERN.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    // ── Extracción ITBMS ──────────────────────────────────────────────────────

    private BigDecimal extractItbms(String text) {
        Matcher m = ITBMS_PATTERN.matcher(text);
        return m.find() ? parseBigDecimal(m.group(1)) : null;
    }

    // ── Subtotal ──────────────────────────────────────────────────────────────

    private BigDecimal calcSubtotal(BigDecimal total, BigDecimal tax) {
        if (total == null)
            return null;
        if (tax == null) {
            // Calcular subtotal asumiendo ITBMS 7%
            return total.divide(BigDecimal.valueOf(1.07), 2, RoundingMode.HALF_UP);
        }
        return total.subtract(tax);
    }

    // ── Items de línea ────────────────────────────────────────────────────────

    private List<OcrLineItem> extractLineItems(String text) {
        List<OcrLineItem> items = new ArrayList<>();
        Matcher m = LINE_ITEM_PATTERN.matcher(text);

        while (m.find()) {
            try {
                items.add(OcrLineItem.builder().description(m.group(1).trim())
                        .quantity(Integer.parseInt(m.group(2).trim()))
                        .unitPrice(parseBigDecimal(m.group(3)))
                        .totalPrice(parseBigDecimal(m.group(4))).build());
            } catch (NumberFormatException ignored) {
            }
        }
        return items;
    }

    // ── Confianza del resultado ───────────────────────────────────────────────

    private double calcConfidence(BigDecimal total, LocalDate date, String merchant) {
        double score = 0.0;
        if (total != null && total.compareTo(BigDecimal.ZERO) > 0)
            score += 0.5;
        if (date != null)
            score += 0.3;
        if (merchant != null && !merchant.equals("Comercio desconocido"))
            score += 0.2;
        return Math.round(score * 100.0) / 100.0;
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private BigDecimal parseBigDecimal(String value) {
        if (value == null)
            return null;
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty())
            return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
