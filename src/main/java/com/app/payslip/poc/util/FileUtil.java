package com.app.payslip.poc.util;

import lombok.experimental.UtilityClass;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@UtilityClass
public class FileUtil {

    public static BufferedImage processFile(MultipartFile file) throws IOException {
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

    private static BufferedImage convertPdfToImage(MultipartFile pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile.getBytes())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            return pdfRenderer.renderImageWithDPI(0, 300);
        }
    }
}
