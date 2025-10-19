package com.app.payslip.poc.model;

import lombok.Builder;
import lombok.Singular;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PayslipComparisonResultDTO(
        String employeeNumber,
        String nationalId,
        String employerNumber,
        String previousMonthKey,
        String currentMonthKey,
        @Singular
        List<FieldChangeDTO> fieldChanges,
        @Singular
        List<BenefitChangeDTO> benefitChanges
) {
    @Builder
    public record FieldChangeDTO(
            String fieldName,
            String description,
            BigDecimal previousValue,
            BigDecimal currentValue,
            BigDecimal delta
    ) {
    }

    @Builder
    public record BenefitChangeDTO(
            String code,
            String label,
            String category,
            String changeType,
            BigDecimal previousAmount,
            BigDecimal currentAmount,
            String previousDirection,
            String currentDirection,
            Boolean previousTaxable,
            Boolean currentTaxable
    ) {
    }
}
