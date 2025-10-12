package com.app.payslip.poc.controller;

import com.app.payslip.poc.model.ExtractedPayslipDataDTO;
import com.app.payslip.poc.service.PayslipExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payslip")
public class PayslipController {

    private final PayslipExtractionService service;

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ExtractedPayslipDataDTO extract(@RequestPart("file") MultipartFile file) throws Exception {
        return service.scrapePaySlip(file);
    }
}
