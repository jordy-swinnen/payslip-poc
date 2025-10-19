package com.app.payslip.poc.service;

import com.app.payslip.poc.config.PromptConfigProperties;
import com.app.payslip.poc.model.ExtractedPayslipDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.app.payslip.poc.util.FileUtil.convertFileToImageBytes;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipExtractionService {

    private final ChatClient chat;
    private final PromptConfigProperties promptConfig;
    private final PayslipIndexService payslipIndexService;

    public ExtractedPayslipDataDTO scrapeAndIndexPayslip(MultipartFile file) throws IOException {
        byte[] imageBytes = convertFileToImageBytes(file);

        ExtractedPayslipDataDTO extractedPayslipData = executePrompt(imageBytes);

        indexPayslipData(file, extractedPayslipData);

        return extractedPayslipData;
    }

    private ExtractedPayslipDataDTO executePrompt(byte[] imageBytes) {
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

    private void indexPayslipData(MultipartFile file, ExtractedPayslipDataDTO extractedPayslipData) {
        try {
            String sourceName = file.getOriginalFilename();
            byte[] originalFileBytes = file.getBytes();
            List<String> documentIds = payslipIndexService.index(extractedPayslipData, sourceName, originalFileBytes);
            log.info("Successfully indexed payslip from file '{}'. Created {} document entries with IDs: {}",
                    sourceName, documentIds.size(), documentIds);
        } catch (Exception indexingException) {
            log.error("Failed to index payslip from file '{}': {}",
                    file.getOriginalFilename(), indexingException.getMessage(), indexingException);
        }
    }
}