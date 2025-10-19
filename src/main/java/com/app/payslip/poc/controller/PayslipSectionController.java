package com.app.payslip.poc.controller;

import com.app.payslip.poc.model.PayslipSectionDTO;
import com.app.payslip.poc.service.PayslipSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/payslip", produces = MediaType.APPLICATION_JSON_VALUE)
public class PayslipSectionController {

    private final PayslipSectionService sectionService;

    @GetMapping("/section/{docId}")
    public ResponseEntity<PayslipSectionDTO> getSection(@PathVariable String docId) {
        return sectionService.getSectionByDocId(docId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}