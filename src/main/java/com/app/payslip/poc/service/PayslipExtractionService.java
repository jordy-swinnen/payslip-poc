package com.app.payslip.poc.service;

import com.app.payslip.poc.config.PromptConfigProperties;
import com.app.payslip.poc.model.ExtractedPayslipDataDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static com.app.payslip.poc.util.FileUtil.processFile;
import static com.app.payslip.poc.util.ImageUtil.convertImageToBytes;

@Service
@RequiredArgsConstructor
public class PayslipExtractionService {

    private final ChatClient chat;
    private final PromptConfigProperties promptConfig;

    public ExtractedPayslipDataDTO scrapePayslip(MultipartFile file) throws IOException {
        BufferedImage image = processFile(file);

        byte[] imageBytes = convertImageToBytes(image);

        return chat.prompt()
                .system(promptConfig.getPayslip().getSystemExtraction())
                .user(userSpec -> userSpec
                        .text(promptConfig.getPayslip().getUserExtraction())
                        .media(Media.builder()
                                .mimeType(MimeTypeUtils.IMAGE_PNG)
                                .data(imageBytes)
                                .build()))
                .call()
                .entity(ExtractedPayslipDataDTO.class);
    }
}