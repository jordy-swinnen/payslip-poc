package com.app.payslip.poc.service;

import com.app.payslip.poc.model.ExtractedPayslipDataDTO;
import com.app.payslip.poc.model.PayslipComparisonResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PayslipComparator {

    @Tool(
            name = "comparePayslips",
            description = "Compares two payslips and returns field and benefit differences. " +
                    "Use when the user asks about changes between months or why net differs."
    )
    public PayslipComparisonResultDTO comparePayslips(ExtractedPayslipDataDTO previousPayslip, ExtractedPayslipDataDTO currentPayslip) {
        PayslipComparisonResultDTO.PayslipComparisonResultDTOBuilder result = PayslipComparisonResultDTO.builder()
                .employeeNumber(Optional.ofNullable(currentPayslip.employment()).map(ExtractedPayslipDataDTO.EmploymentInfo::employeeNumber).orElse(""))
                .nationalId(Optional.ofNullable(currentPayslip.personal()).map(ExtractedPayslipDataDTO.PersonalInfo::nationalId).orElse(""))
                .employerNumber(Optional.ofNullable(currentPayslip.employer()).map(ExtractedPayslipDataDTO.EmployerInfo::employerNumber).orElse(""))
                .previousMonthKey(monthKey(Optional.ofNullable(previousPayslip.period()).map(ExtractedPayslipDataDTO.PeriodInfo::periodStart).orElse(null)))
                .currentMonthKey(monthKey(Optional.ofNullable(currentPayslip.period()).map(ExtractedPayslipDataDTO.PeriodInfo::periodStart).orElse(null)));

        addFieldChange(result, "baseMonthlySalary", "Base monthly salary",
                Optional.ofNullable(previousPayslip.employment()).map(ExtractedPayslipDataDTO.EmploymentInfo::baseMonthlySalary).orElse(null),
                Optional.ofNullable(currentPayslip.employment()).map(ExtractedPayslipDataDTO.EmploymentInfo::baseMonthlySalary).orElse(null));

        addFieldChange(result, "gross", "Gross pay",
                Optional.ofNullable(previousPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::gross).orElse(null),
                Optional.ofNullable(currentPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::gross).orElse(null));

        addFieldChange(result, "taxable", "Taxable pay",
                Optional.ofNullable(previousPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::taxable).orElse(null),
                Optional.ofNullable(currentPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::taxable).orElse(null));

        addFieldChange(result, "socialSecurity", "Social security (RSZ)",
                Optional.ofNullable(previousPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::socialSecurity).orElse(null),
                Optional.ofNullable(currentPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::socialSecurity).orElse(null));

        addFieldChange(result, "withholdingTax", "Withholding tax",
                Optional.ofNullable(previousPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::withholdingTax).orElse(null),
                Optional.ofNullable(currentPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::withholdingTax).orElse(null));

        addFieldChange(result, "net", "Net pay",
                Optional.ofNullable(previousPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::net).orElse(null),
                Optional.ofNullable(currentPayslip.financial()).map(ExtractedPayslipDataDTO.FinancialInfo::net).orElse(null));

        addFieldChange(result, "mealVoucherEmployer", "Meal vouchers (employer contribution)",
                Optional.ofNullable(previousPayslip.extras()).map(ExtractedPayslipDataDTO.Extras::mealVoucherContributionEmployer).orElse(null),
                Optional.ofNullable(currentPayslip.extras()).map(ExtractedPayslipDataDTO.Extras::mealVoucherContributionEmployer).orElse(null));

        addFieldChange(result, "mealVoucherEmployee", "Meal vouchers (employee contribution)",
                Optional.ofNullable(previousPayslip.extras()).map(ExtractedPayslipDataDTO.Extras::mealVoucherContributionEmployee).orElse(null),
                Optional.ofNullable(currentPayslip.extras()).map(ExtractedPayslipDataDTO.Extras::mealVoucherContributionEmployee).orElse(null));

        addFieldChange(result, "mealVoucherCount", "Meal voucher count",
                toBigDecimal(Optional.ofNullable(previousPayslip.extras()).map(ExtractedPayslipDataDTO.Extras::mealVoucherCount).orElse(null)),
                toBigDecimal(Optional.ofNullable(currentPayslip.extras()).map(ExtractedPayslipDataDTO.Extras::mealVoucherCount).orElse(null)));

        List<PayslipComparisonResultDTO.BenefitChangeDTO> benefitChanges =
                compareBenefits(
                        Optional.ofNullable(previousPayslip.extras()).map(ExtractedPayslipDataDTO.Extras::benefits).orElse(List.of()),
                        Optional.ofNullable(currentPayslip.extras()).map(ExtractedPayslipDataDTO.Extras::benefits).orElse(List.of())
                );

        benefitChanges.forEach(result::benefitChange);

        return result.build();
    }

    private void addFieldChange(PayslipComparisonResultDTO.PayslipComparisonResultDTOBuilder result,
                                String fieldName,
                                String description,
                                BigDecimal previousValue,
                                BigDecimal currentValue
    ) {
        BigDecimal safePrevious = previousValue == null ? BigDecimal.ZERO : previousValue;
        BigDecimal safeCurrent = currentValue == null ? BigDecimal.ZERO : currentValue;
        if (safePrevious.compareTo(safeCurrent) != 0) {
            result.fieldChange(PayslipComparisonResultDTO.FieldChangeDTO.builder()
                    .fieldName(fieldName)
                    .description(description)
                    .previousValue(safePrevious)
                    .currentValue(safeCurrent)
                    .delta(safeCurrent.subtract(safePrevious))
                    .build());
        }
    }

    private List<PayslipComparisonResultDTO.BenefitChangeDTO> compareBenefits(
            List<ExtractedPayslipDataDTO.Extras.Benefit> previousBenefits,
            List<ExtractedPayslipDataDTO.Extras.Benefit> currentBenefits
    ) {
        Map<String, ExtractedPayslipDataDTO.Extras.Benefit> previousMap = keyByCodeOrLabel(previousBenefits);
        Map<String, ExtractedPayslipDataDTO.Extras.Benefit> currentMap = keyByCodeOrLabel(currentBenefits);

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(previousMap.keySet());
        allKeys.addAll(currentMap.keySet());

        List<PayslipComparisonResultDTO.BenefitChangeDTO> changes = new ArrayList<>();

        for (String key : allKeys) {
            ExtractedPayslipDataDTO.Extras.Benefit prev = previousMap.get(key);
            ExtractedPayslipDataDTO.Extras.Benefit curr = currentMap.get(key);

            if (prev == null && curr != null) {
                changes.add(PayslipComparisonResultDTO.BenefitChangeDTO.builder()
                        .code(curr.code())
                        .label(curr.label())
                        .category(curr.category())
                        .changeType("ADDED")
                        .currentAmount(curr.amount())
                        .currentDirection(curr.direction())
                        .currentTaxable(Boolean.TRUE.equals(curr.taxable()))
                        .build());
                continue;
            }
            if (prev != null && curr == null) {
                changes.add(PayslipComparisonResultDTO.BenefitChangeDTO.builder()
                        .code(prev.code())
                        .label(prev.label())
                        .category(prev.category())
                        .changeType("REMOVED")
                        .previousAmount(prev.amount())
                        .previousDirection(prev.direction())
                        .previousTaxable(Boolean.TRUE.equals(prev.taxable()))
                        .build());
                continue;
            }
            if (prev != null && curr != null) {
                boolean amountChanged = notEqual(prev.amount(), curr.amount());
                boolean directionChanged = notEqual(prev.direction(), curr.direction());
                boolean taxableChanged = !Objects.equals(Boolean.TRUE.equals(prev.taxable()), Boolean.TRUE.equals(curr.taxable()));
                if (amountChanged || directionChanged || taxableChanged) {
                    changes.add(PayslipComparisonResultDTO.BenefitChangeDTO.builder()
                            .code(curr.code())
                            .label(curr.label())
                            .category(curr.category())
                            .changeType(combineChangeType(amountChanged, directionChanged, taxableChanged))
                            .previousAmount(prev.amount())
                            .currentAmount(curr.amount())
                            .previousDirection(prev.direction())
                            .currentDirection(curr.direction())
                            .previousTaxable(Boolean.TRUE.equals(prev.taxable()))
                            .currentTaxable(Boolean.TRUE.equals(curr.taxable()))
                            .build());
                }
            }
        }
        return changes;
    }

    private Map<String, ExtractedPayslipDataDTO.Extras.Benefit> keyByCodeOrLabel(List<ExtractedPayslipDataDTO.Extras.Benefit> benefits) {
        return benefits.stream().collect(Collectors.toMap(
                b -> Optional.ofNullable(b.code()).filter(s -> !s.isBlank()).orElse(
                        Optional.ofNullable(b.label()).orElse(UUID.randomUUID().toString())),
                Function.identity(),
                (left, right) -> right,
                LinkedHashMap::new
        ));
    }

    private boolean notEqual(BigDecimal left, BigDecimal right) {
        BigDecimal a = left == null ? BigDecimal.ZERO : left;
        BigDecimal b = right == null ? BigDecimal.ZERO : right;
        return a.compareTo(b) != 0;
    }

    private boolean notEqual(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        return !Objects.equals(a, b);
    }

    private String combineChangeType(boolean amountChanged, boolean directionChanged, boolean taxableChanged) {
        List<String> parts = new ArrayList<>();
        if (amountChanged) parts.add("AMOUNT_CHANGED");
        if (directionChanged) parts.add("DIRECTION_CHANGED");
        if (taxableChanged) parts.add("TAXABLE_CHANGED");
        return String.join("+", parts);
    }

    private String monthKey(java.time.LocalDate start) {
        if (start == null) return "";
        return "%d-%02d".formatted(start.getYear(), start.getMonthValue());
    }

    private BigDecimal toBigDecimal(Integer value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value);
    }
}
