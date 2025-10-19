package com.app.payslip.poc.controller;

import com.app.payslip.poc.model.TextAnswerDTO;
import com.app.payslip.poc.service.PayslipCompareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/payslip")
@RequiredArgsConstructor
public class PayslipCompareController {

    private final PayslipCompareService payslipCompareService;

    @PostMapping(
            value = "/chat",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public TextAnswerDTO chat(
            @RequestPart("previous") MultipartFile previousPayslipFile,
            @RequestPart("current") MultipartFile currentPayslipFile,
            @RequestPart("question") String question) throws IOException {
        return payslipCompareService.compareAndExplain(previousPayslipFile, currentPayslipFile, question);
    }
}
