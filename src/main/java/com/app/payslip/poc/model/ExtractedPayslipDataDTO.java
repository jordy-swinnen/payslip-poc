package com.app.payslip.poc.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record ExtractedPayslipDataDTO(
        String employeeName,
        String employerName,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate payDate,
        String currency,
        BigDecimal gross,
        BigDecimal net
) {
}
