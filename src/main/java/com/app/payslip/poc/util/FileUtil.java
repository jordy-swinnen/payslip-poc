package com.app.payslip.poc.util;

import lombok.experimental.UtilityClass;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@UtilityClass
public class FileUtil {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";
    private static final String PNG_FORMAT = "PNG";
    private static final int PDF_RENDERING_DPI = 300;
    private static final int FIRST_PAGE_INDEX = 0;

    public static byte[] convertFileToImageBytes(MultipartFile file) throws IOException {
        String contentType = getContentType(file);

        if (isPdf(contentType)) {
            return convertPdfToImageBytes(file);
        }

        if (isImage(contentType)) {
            return convertImageFileToBytes(file);
        }

        throw new UnsupportedOperationException(
                "Unsupported file type: " + contentType + ". Only PDF and image files are supported."
        );
    }

    private static String getContentType(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new UnsupportedOperationException("Unable to determine file type");
        }
        return contentType;
    }

    private static boolean isPdf(String contentType) {
        return PDF_CONTENT_TYPE.equals(contentType);
    }

    private static boolean isImage(String contentType) {
        return contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX);
    }

    private static byte[] convertPdfToImageBytes(MultipartFile pdfFile) throws IOException {
        BufferedImage image = convertPdfToImage(pdfFile);
        return convertImageToBytes(image);
    }

    private static byte[] convertImageFileToBytes(MultipartFile imageFile) throws IOException {
        BufferedImage image = readImage(imageFile);
        validateImage(image);
        return convertImageToBytes(image);
    }

    private static BufferedImage convertPdfToImage(MultipartFile pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile.getBytes())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            return pdfRenderer.renderImageWithDPI(FIRST_PAGE_INDEX, PDF_RENDERING_DPI);
        }
    }

    private static BufferedImage readImage(MultipartFile imageFile) throws IOException {
        return ImageIO.read(imageFile.getInputStream());
    }

    private static void validateImage(BufferedImage image) {
        if (image == null) {
            throw new UnsupportedOperationException("Invalid or unsupported image format");
        }
    }

    private static byte[] convertImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, PNG_FORMAT, outputStream);
        return outputStream.toByteArray();
    }
}
