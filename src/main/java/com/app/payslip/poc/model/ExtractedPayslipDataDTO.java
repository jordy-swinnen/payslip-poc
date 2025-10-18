package com.app.payslip.poc.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record ExtractedPayslipDataDTO(
        PersonalInfo personal,
        EmployerInfo employer,
        EmploymentInfo employment,
        PeriodInfo period,
        FinancialInfo financial,
        Extras extras
) {

    @Builder
    public record PersonalInfo(
            String name,
            String address,
            String nationalId,      // INSZ/NISS
            String maritalStatus,
            Integer dependents
    ) {
    }

    @Builder
    public record EmployerInfo(
            String name,
            String address,
            String employerNumber
    ) {
    }

    @Builder
    public record EmploymentInfo(
            String employeeNumber,
            String jobTitle,
            String status,
            String payCategory,
            BigDecimal baseMonthlySalary
    ) {
    }

    @Builder
    public record PeriodInfo(
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate payDate,
            String currency
    ) {
    }

    @Builder
    public record FinancialInfo(
            BigDecimal gross,
            BigDecimal taxable,
            BigDecimal socialSecurity,
            BigDecimal withholdingTax,
            BigDecimal net,
            String paymentIban,
            String paymentBic
    ) {
    }

    @Builder
    public record Extras(
            BigDecimal mealVoucherContributionEmployer,
            BigDecimal mealVoucherContributionEmployee,
            Integer mealVoucherCount,
            List<Benefit> benefits
    ) {
        @Builder
        public record Benefit(
                String code,
                String label,
                String category,
                BigDecimal amount,
                String direction,
                Boolean taxable
        ) {
        }
    }
}
