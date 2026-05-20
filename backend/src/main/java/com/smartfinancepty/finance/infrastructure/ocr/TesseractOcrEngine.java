package com.smartfinancepty.finance.infrastructure.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Component
@Slf4j
public class TesseractOcrEngine {

    @Value("${app.ocr.tessdata-path:/usr/share/tesseract-ocr/5/tessdata}")
    private String tessdataPath;

    @Value("${app.ocr.language:spa+eng}")
    private String language;

    @Value("${app.ocr.dpi:300}")
    private int dpi;

    /**
     * Extrae texto de una imagen usando Tesseract OCR
     */
    public String extractText(MultipartFile file) throws Exception {
        String contentType = file.getContentType();

        if ("image/webp".equals(contentType)) {
            // Convertir WebP → PNG via ImageMagick
            Path temp = Files.createTempFile("ocr_", ".webp");
            Path converted = Files.createTempFile("ocr_", ".png");
            try {
                file.transferTo(temp.toFile());
                ProcessBuilder pb =
                        new ProcessBuilder("convert", temp.toString(), converted.toString());
                pb.start().waitFor();
                return extractTextFromFile(converted.toFile());
            } finally {
                Files.deleteIfExists(temp);
                Files.deleteIfExists(converted);
            }
        }

        BufferedImage image = ImageIO.read(file.getInputStream());
        return extractTextFromImage(image);
    }

    /**
     * Extrae texto de una ruta de archivo
     */
    public String extractTextFromFile(File imageFile) throws TesseractException {
        Tesseract tesseract = buildTesseract();

        long start = System.currentTimeMillis();
        String result = tesseract.doOCR(imageFile);
        long elapsed = System.currentTimeMillis() - start;

        log.info("🔍 OCR completado en {}ms — {} caracteres extraídos", elapsed, result.length());

        return result;
    }

    /**
     * Extrae texto de una imagen en memoria
     */
    public String extractTextFromImage(BufferedImage image) throws TesseractException {
        Tesseract tesseract = buildTesseract();
        return tesseract.doOCR(image);
    }

    private Tesseract buildTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(language);
        tesseract.setPageSegMode(6); // Uniform block of text
        tesseract.setOcrEngineMode(1); // LSTM neural network
        tesseract.setVariable("user_defined_dpi", String.valueOf(dpi));
        return tesseract;
    }
}
