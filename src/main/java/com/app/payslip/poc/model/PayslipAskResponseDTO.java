package com.app.payslip.poc.model;

import lombok.Builder;

import java.util.List;

@Builder
public record PayslipAskResponseDTO(String answer, List<PayslipAskCitationDTO> sources) {
    @Builder
    public record PayslipAskCitationDTO(
            String docId,
            String section,
            String source,
            String periodMonthKey,
            String employerNumber,
            String employeeNumber,
            String nationalId,
            String textPreview,
            String url
    ) {
    }
}