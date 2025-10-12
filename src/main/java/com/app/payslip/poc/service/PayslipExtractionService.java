package com.app.payslip.poc.service;

import com.app.payslip.poc.model.ExtractedPayslipDataDTO;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PayslipExtractionService {

    private final ChatClient chat;

    public ExtractedPayslipDataDTO scrapePaySlip(MultipartFile file) throws IOException {
        BufferedImage image = processFile(file);

        byte[] imageBytes = convertImageToBytes(image);

        return chat.prompt()
                .system(systemPrompt())
                .user(userSpec -> userSpec
                        .text("Extract the payslip data from this image.")
                        .media(Media.builder()
                                .mimeType(MimeTypeUtils.IMAGE_PNG)
                                .data(imageBytes)
                                .build()))
                .call()
                .entity(ExtractedPayslipDataDTO.class);
    }

    private static String systemPrompt() {
        return """
                You are an expert at extracting payslip data.
                Extract the following information from the payslip image:
                - Employee name
                - Employer name
                - Pay period start date
                - Pay period end date
                - Payment date
                - Currency (ISO code like USD, EUR, GBP)
                - Gross pay amount
                - Net pay amount
                
                Return the data as JSON matching this structure:
                {
                  "employeeName": "string",
                  "employerName": "string",
                  "periodStart": "YYYY-MM-DD",
                  "periodEnd": "YYYY-MM-DD",
                  "payDate": "YYYY-MM-DD",
                  "currency": "string",
                  "gross": number,
                  "net": number
                }
                
                If any field is not found, use null for that field.
                """;
    }

    private BufferedImage processFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if (contentType == null) {
            throw new UnsupportedOperationException("Unable to determine file type");
        }

        if (contentType.equals("application/pdf")) {
            return convertPdfToImage(file);
        }

        if (contentType.startsWith("image/")) {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new UnsupportedOperationException("Invalid or unsupported image format");
            }
            return image;
        }

        throw new UnsupportedOperationException("Unsupported file type: " + contentType + ". Only PDF and image files are supported.");
    }

    private BufferedImage convertPdfToImage(MultipartFile pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile.getBytes())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            return pdfRenderer.renderImageWithDPI(0, 300);
        }
    }

    private byte[] convertImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}