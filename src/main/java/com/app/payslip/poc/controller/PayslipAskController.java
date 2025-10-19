package com.app.payslip.poc.controller;

import com.app.payslip.poc.model.PayslipAskResponseDTO;
import com.app.payslip.poc.service.PayslipAskService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/payslip")
public class PayslipAskController {

    private final PayslipAskService payslipAskService;

    public PayslipAskController(PayslipAskService payslipAskService) {
        this.payslipAskService = payslipAskService;
    }

    @PostMapping(value = "/ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PayslipAskResponseDTO> ask(@RequestPart("file") MultipartFile payslipFile, @RequestPart("question") String question) throws IOException {
        return ResponseEntity.ok(payslipAskService.answer(payslipFile, question));
    }
}
