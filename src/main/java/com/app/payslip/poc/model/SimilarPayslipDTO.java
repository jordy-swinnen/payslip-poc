package com.app.payslip.poc.model;

import lombok.Builder;

import java.util.List;

@Builder
public record SimilarPayslipDTO(
        String payslipId,
        String nationalId,
        String name,
        String monthKey,
        String payDate,
        String source,
        List<String> documentIds
) {
}