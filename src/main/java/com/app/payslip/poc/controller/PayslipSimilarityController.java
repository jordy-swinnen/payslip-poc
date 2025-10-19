package com.app.payslip.poc.controller;

import com.app.payslip.poc.model.SimilarPayslipDTO;
import com.app.payslip.poc.service.PayslipSimilarityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/payslip", produces = MediaType.APPLICATION_JSON_VALUE)
public class PayslipSimilarityController {

    private final PayslipSimilarityService similarityService;

    @GetMapping("/similar")
    public List<SimilarPayslipDTO> findSimilar(
            @RequestParam(value = "nationalId", required = false) String nationalId,
            @RequestParam(value = "employeeName", required = false) String employeeName,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    ) {
        return similarityService.payslipSimilaritySearch(nationalId, employeeName, limit);
    }
}
