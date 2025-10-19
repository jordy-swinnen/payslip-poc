package com.app.payslip.poc.controller;

import com.app.payslip.poc.model.PayslipAskResponseDTO;
import com.app.payslip.poc.service.PayslipAskService;
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
public class PayslipAskController {

    private final PayslipAskService service;

    @PostMapping(value = "/ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PayslipAskResponseDTO ask(@RequestPart("file") MultipartFile payslipFile, @RequestPart("question") String question) throws IOException {
        return service.ask(payslipFile, question);
    }
}
