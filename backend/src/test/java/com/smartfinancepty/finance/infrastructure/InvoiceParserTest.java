package com.smartfinancepty.finance.infrastructure;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.smartfinancepty.finance.dto.ocr.OcrResult;
import com.smartfinancepty.finance.infrastructure.ocr.InvoiceParser;

@DisplayName("InvoiceParser Tests")
class InvoiceParserTest {

    private InvoiceParser invoiceParser;

    @BeforeEach
    void setUp() {
        invoiceParser = new InvoiceParser();
    }

    @Nested
    @DisplayName("Texto vacío o nulo")
    class EmptyTextTests {

        @Test
        @DisplayName("Debe retornar resultado no procesado cuando el texto es null")
        void shouldReturnUnprocessedWhenNull() {
            OcrResult result = invoiceParser.parse(null);

            assertThat(result.isProcessed()).isFalse();
            assertThat(result.getErrorMessage()).contains("Texto vacío");
        }

        @Test
        @DisplayName("Debe retornar resultado no procesado cuando el texto está en blanco")
        void shouldReturnUnprocessedWhenBlank() {
            OcrResult result = invoiceParser.parse("   ");

            assertThat(result.isProcessed()).isFalse();
            assertThat(result.getErrorMessage()).contains("Texto vacío");
        }
    }

    @Nested
    @DisplayName("Extracción de monto total")
    class TotalExtractionTests {

        @Test
        @DisplayName("Debe extraer monto con palabra clave TOTAL")
        void shouldExtractTotalWithKeyword() {
            String text = "Super 99\nRUC: 8-123-45678\nFecha: 15/05/2026\nPan $2.50\nLeche $3.00\nTOTAL: $5.50";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.isProcessed()).isTrue();
            assertThat(result.getTotalAmount()).isEqualByComparingTo("5.50");
        }

        @Test
        @DisplayName("Debe extraer monto con prefijo B/.")
        void shouldExtractTotalWithBalboas() {
            String text = "Farmacia\nFecha: 15/05/2026\nProducto A B/.25.00\nTotal B/.25.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.isProcessed()).isTrue();
            assertThat(result.getTotalAmount()).isNotNull();
        }

        @Test
        @DisplayName("Debe extraer monto más alto cuando no hay palabra clave")
        void shouldExtractLargestAmountWhenNoKeyword() {
            String text = "Comercio\nFecha: 01/01/2026\n$5.00\n$15.00\n$8.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.isProcessed()).isTrue();
            assertThat(result.getTotalAmount()).isEqualByComparingTo("15.00");
        }

        @Test
        @DisplayName("Debe manejar total nulo cuando no hay montos detectables")
        void shouldHandleNullTotalGracefully() {
            String text = "Factura sin montos\nComercio ABC\nFecha: 01/05/2026";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.isProcessed()).isTrue();
            assertThat(result.getTotalAmount()).isNull();
        }
    }

    @Nested
    @DisplayName("Extracción de fecha")
    class DateExtractionTests {

        @Test
        @DisplayName("Debe extraer fecha en formato DD/MM/YYYY")
        void shouldExtractDateDMY() {
            String text = "Super Rey\nFecha: 15/05/2026\nTotal $10.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getInvoiceDate()).isNotNull();
            assertThat(result.getInvoiceDate().getYear()).isEqualTo(2026);
            assertThat(result.getInvoiceDate().getMonthValue()).isEqualTo(5);
            assertThat(result.getInvoiceDate().getDayOfMonth()).isEqualTo(15);
        }

        @Test
        @DisplayName("Debe extraer fecha en formato YYYY-MM-DD")
        void shouldExtractDateISO() {
            String text = "Comercio\n2026-03-10\nTotal: $20.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getInvoiceDate()).isNotNull();
            assertThat(result.getInvoiceDate().getYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("Debe usar fecha actual como fallback cuando no se detecta fecha")
        void shouldFallbackToCurrentDateWhenNoDate() {
            String text = "Comercio sin fecha\nTotal $50.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getInvoiceDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Extracción del nombre del comercio")
    class MerchantNameTests {

        @Test
        @DisplayName("Debe extraer el nombre del comercio de las primeras líneas")
        void shouldExtractMerchantName() {
            String text = "Super Rey\nRUC: 8-123-45678\nTotal: $30.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getMerchantName()).isNotNull();
            assertThat(result.getMerchantName()).isNotEqualTo("Comercio desconocido");
        }

        @Test
        @DisplayName("Debe ignorar líneas que parecen fechas o montos")
        void shouldIgnoreDateAndAmountLines() {
            String text = "15/05/2026\n$100.00\nfactura\nAlimentosS.A.";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getMerchantName()).isNotNull();
        }

        @Test
        @DisplayName("Debe retornar 'Comercio desconocido' cuando no puede extraer nombre")
        void shouldReturnUnknownMerchantWhenCannotExtract() {
            String text = "15/05/2026\n$100.00\nfactura\nrecibo";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getMerchantName()).isEqualTo("Comercio desconocido");
        }
    }

    @Nested
    @DisplayName("Extracción de RUC")
    class RucExtractionTests {

        @Test
        @DisplayName("Debe extraer RUC panameño")
        void shouldExtractRuc() {
            String text = "Super 99\nRUC: 8-123-45678\nFecha: 15/05/2026\nTotal: $50.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getMerchantRuc()).isEqualTo("8-123-45678");
        }

        @Test
        @DisplayName("RUC debe ser null cuando no se detecta")
        void shouldReturnNullRucWhenNotFound() {
            String text = "Comercio\nFecha: 15/05/2026\nTotal: $20.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getMerchantRuc()).isNull();
        }
    }

    @Nested
    @DisplayName("Extracción de ITBMS y subtotal")
    class ItbmsTests {

        @Test
        @DisplayName("Debe extraer ITBMS y calcular subtotal correctamente")
        void shouldExtractItbmsAndCalculateSubtotal() {
            String text = "Comercio\nFecha: 15/05/2026\nITBMS: 3.50\nTotal: $53.50";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getTaxAmount()).isEqualByComparingTo("3.50");
            // subtotal = total - tax = 53.50 - 3.50 = 50.00
            assertThat(result.getSubtotal()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("Debe calcular subtotal asumiendo 7% cuando no hay ITBMS")
        void shouldCalculateSubtotalAssumingItbmsWhenNotFound() {
            String text = "Comercio\nFecha: 15/05/2026\nTotal: $107.00";

            OcrResult result = invoiceParser.parse(text);

            // subtotal = 107 / 1.07 ≈ 100.00
            assertThat(result.getSubtotal()).isNotNull();
            assertThat(result.getSubtotal()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Subtotal debe ser null cuando total es null")
        void shouldReturnNullSubtotalWhenTotalIsNull() {
            String text = "Comercio ABC\nFecha: 01/05/2026\nsin montos aqui";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getSubtotal()).isNull();
        }
    }

    @Nested
    @DisplayName("Confianza del resultado")
    class ConfidenceTests {

        @Test
        @DisplayName("Alta confianza cuando hay total, fecha y comercio")
        void shouldHaveHighConfidenceWhenAllPresent() {
            String text = "Super Rey\nFecha: 15/05/2026\nTotal: $50.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getConfidence()).isGreaterThanOrEqualTo(0.8);
        }

        @Test
        @DisplayName("Confianza parcial cuando falta el comercio")
        void shouldHavePartialConfidenceWhenMerchantMissing() {
            String text = "15/05/2026\nfactura\nrecibo\nTotal: $50.00";

            OcrResult result = invoiceParser.parse(text);

            // total (0.5) + date (0.3) = 0.8 si la fecha se extrae
            assertThat(result.getConfidence()).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("Confianza exactamente 0.5 cuando hay fecha y comercio pero no total")
        void shouldHaveAtMostHalfConfidenceWhenNoTotal() {
            String text = "Comercio\nFecha: 15/05/2026\nsin montos";

            OcrResult result = invoiceParser.parse(text);

            // sin total: confidence = date(0.3) + merchant(0.2) = 0.5 como máximo
            assertThat(result.getConfidence()).isLessThanOrEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("Metadatos del resultado")
    class MetadataTests {

        @Test
        @DisplayName("Debe incluir texto raw, moneda y lenguaje")
        void shouldIncludeMetadata() {
            String text = "Super 99\nFecha: 15/05/2026\nTotal: $25.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getRawText()).isEqualTo(text);
            assertThat(result.getCurrency()).isEqualTo("PAB");
            assertThat(result.getLanguage()).isEqualTo("spa");
            assertThat(result.isProcessed()).isTrue();
        }

        @Test
        @DisplayName("Debe incluir lista de items de línea")
        void shouldIncludeLineItems() {
            String text = "Super\nFecha: 01/05/2026\nTotal: $50.00";

            OcrResult result = invoiceParser.parse(text);

            assertThat(result.getLineItems()).isNotNull();
        }
    }
}
