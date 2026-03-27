package com.incident.classifier.util;

import com.incident.classifier.exception.DocumentProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Extracts raw text from PDF files using Apache PDFBox.
 */
@Component
public class PdfExtractor {

    /**
     * Extracts text from a PDF MultipartFile.
     *
     * @param file the uploaded PDF
     * @return extracted plain text
     * @throws DocumentProcessingException if extraction fails
     */
    public String extract(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.isEncrypted()) {
                throw new DocumentProcessingException("The uploaded PDF is encrypted and cannot be processed.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new DocumentProcessingException("No readable text found in the uploaded PDF.");
            }
            return text;
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read PDF: " + e.getMessage(), e);
        }
    }
}
