package com.app.payslip.poc.service;

import com.app.payslip.poc.model.ExtractedPayslipDataDTO;
import com.app.payslip.poc.model.ExtractedPayslipDataDTO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayslipIndexService {

    private static final int MAX_ADDRESS_DISPLAY_LENGTH = 80;

    private final VectorStore vectorStore;

    public List<String> index(ExtractedPayslipDataDTO payslipData, String sourceName, byte[] originalBytes) {
        Map<String, Object> baseMetadata = baseMetadata(payslipData, sourceName, originalBytes);

        List<Document> documents = new ArrayList<>();
        addIfNotBlank(documents, personalText(payslipData), with(baseMetadata, Map.of("section", "personal")));
        addIfNotBlank(documents, employerText(payslipData), with(baseMetadata, Map.of("section", "employer")));
        addIfNotBlank(documents, employmentText(payslipData), with(baseMetadata, Map.of("section", "employment")));
        addIfNotBlank(documents, periodText(payslipData), with(baseMetadata, Map.of("section", "period")));
        addIfNotBlank(documents, financialText(payslipData), with(baseMetadata, Map.of("section", "financial")));
        addIfNotBlank(documents, extrasHeaderText(payslipData), with(baseMetadata, Map.of("section", "extras")));
        benefitDocuments(payslipData).forEach(documentPart -> documents.add(new Document(documentPart.text, with(baseMetadata, documentPart.metadata))));

        String baseId = composeBaseId(baseMetadata);
        int index = 0;
        for (Document document : documents) {
            String hash = shortSha256(document.getText());
            document.getMetadata().put(
                    "docId",
                    baseId + ":" + document.getMetadata().getOrDefault("section", "section") + ":" + hash + ":" + (index++)
            );
        }

        vectorStore.add(documents);
        return documents.stream().map(document -> String.valueOf(document.getMetadata().get("docId"))).toList();
    }

    private String personalText(ExtractedPayslipDataDTO payslipData) {
        PersonalInfo personalInfo = payslipData.personal();
        if (personalInfo == null) return "";
        return """
                PERSONAL
                Employee: %s
                National ID: %s
                Marital status: %s
                Dependents: %s
                Address: %s
                """.formatted(
                toStringOrEmpty(personalInfo.name()),
                toStringOrEmpty(personalInfo.nationalId()),
                toStringOrEmpty(personalInfo.maritalStatus()),
                Optional.ofNullable(personalInfo.dependents()).map(String::valueOf).orElse(""),
                shortAddress(toStringOrEmpty(personalInfo.address()))
        );
    }

    private String employerText(ExtractedPayslipDataDTO payslipData) {
        EmployerInfo employerInfo = payslipData.employer();
        if (employerInfo == null) return "";
        return """
                EMPLOYER
                Name: %s
                Number: %s
                Address: %s
                """.formatted(
                toStringOrEmpty(employerInfo.name()),
                toStringOrEmpty(employerInfo.employerNumber()),
                toStringOrEmpty(employerInfo.address())
        );
    }

    private String employmentText(ExtractedPayslipDataDTO payslipData) {
        EmploymentInfo employmentInfo = payslipData.employment();
        if (employmentInfo == null) return "";
        return """
                EMPLOYMENT
                Employee number: %s
                Job title: %s
                Status: %s
                Pay category: %s
                Base monthly salary: %s
                """.formatted(
                toStringOrEmpty(employmentInfo.employeeNumber()),
                toStringOrEmpty(employmentInfo.jobTitle()),
                toStringOrEmpty(employmentInfo.status()),
                toStringOrEmpty(employmentInfo.payCategory()),
                moneyString(employmentInfo.baseMonthlySalary())
        );
    }

    private String periodText(ExtractedPayslipDataDTO payslipData) {
        PeriodInfo periodInfo = payslipData.period();
        if (periodInfo == null) return "";
        return """
                PERIOD
                Start: %s
                End: %s
                Pay date: %s
                Currency: %s
                """.formatted(
                dateString(periodInfo.periodStart()),
                dateString(periodInfo.periodEnd()),
                dateString(periodInfo.payDate()),
                toStringOrEmpty(periodInfo.currency())
        );
    }

    private String financialText(ExtractedPayslipDataDTO payslipData) {
        FinancialInfo financialInfo = payslipData.financial();
        if (financialInfo == null) return "";
        return """
                FINANCIAL
                Gross: %s
                Taxable: %s
                Social security (RSZ): %s
                Withholding tax: %s
                Net: %s
                Payment IBAN (last4): %s
                Payment BIC (last4): %s
                """.formatted(
                moneyString(financialInfo.gross()),
                moneyString(financialInfo.taxable()),
                moneyString(financialInfo.socialSecurity()),
                moneyString(financialInfo.withholdingTax()),
                moneyString(financialInfo.net()),
                last4(toStringOrEmpty(financialInfo.paymentIban())),
                last4(toStringOrEmpty(financialInfo.paymentBic()))
        );
    }

    private String extrasHeaderText(ExtractedPayslipDataDTO payslipData) {
        Extras extras = payslipData.extras();
        if (extras == null) return "";
        return """
                EXTRAS
                Meal voucher (employer): %s
                Meal voucher (employee): %s
                Meal voucher count: %s
                """.formatted(
                moneyString(extras.mealVoucherContributionEmployer()),
                moneyString(extras.mealVoucherContributionEmployee()),
                Optional.ofNullable(extras.mealVoucherCount()).map(String::valueOf).orElse("")
        );
    }

    private List<DocPart> benefitDocuments(ExtractedPayslipDataDTO payslipData) {
        Extras extras = payslipData.extras();
        if (extras == null || extras.benefits() == null || extras.benefits().isEmpty()) return List.of();

        List<DocPart> documentParts = new ArrayList<>();
        for (ExtractedPayslipDataDTO.Extras.Benefit benefit : extras.benefits()) {
            String text = """
                    BENEFIT
                    Code: %s
                    Label: %s
                    Category: %s
                    Amount: %s
                    Direction: %s
                    Taxable: %s
                    """.formatted(
                    toStringOrEmpty(benefit.code()),
                    toStringOrEmpty(benefit.label()),
                    toStringOrEmpty(benefit.category()),
                    moneyString(benefit.amount()),
                    toStringOrEmpty(benefit.direction()),
                    String.valueOf(Boolean.TRUE.equals(benefit.taxable()))
            );
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("section", "benefit");
            metadata.put("benefit.code", toStringOrEmpty(benefit.code()));
            metadata.put("benefit.label", toStringOrEmpty(benefit.label()));
            metadata.put("benefit.category", toStringOrEmpty(benefit.category()));
            metadata.put("benefit.direction", toStringOrEmpty(benefit.direction()));
            metadata.put("benefit.taxable", Boolean.TRUE.equals(benefit.taxable()));
            metadata.put("benefit.amount", numberOrZero(benefit.amount()));
            documentParts.add(new DocPart(text, metadata));
        }
        return documentParts;
    }

    private Map<String, Object> baseMetadata(ExtractedPayslipDataDTO payslipData, String sourceName, byte[] originalBytes) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        Optional.ofNullable(sourceName).ifPresent(source -> metadata.put("source", source));
        Optional.ofNullable(originalBytes).filter(bytes -> bytes.length > 0).ifPresent(bytes -> {
            metadata.put("file.size", bytes.length);
            metadata.put("file.sha256", sha256Hex(bytes));
        });

        Optional.ofNullable(payslipData.personal()).ifPresent(personalInfo -> {
            metadata.put("personal.name", toStringOrEmpty(personalInfo.name()));
            metadata.put("personal.nationalId", toStringOrEmpty(personalInfo.nationalId()));
            metadata.put("personal.maritalStatus", toStringOrEmpty(personalInfo.maritalStatus()));
            metadata.put("personal.address", toStringOrEmpty(personalInfo.address()));
            metadata.put("personal.dependents", Optional.ofNullable(personalInfo.dependents()).orElse(0));
        });

        Optional.ofNullable(payslipData.employer()).ifPresent(employerInfo -> {
            metadata.put("employer.name", toStringOrEmpty(employerInfo.name()));
            metadata.put("employer.number", toStringOrEmpty(employerInfo.employerNumber()));
            metadata.put("employer.address", toStringOrEmpty(employerInfo.address()));
        });

        Optional.ofNullable(payslipData.employment()).ifPresent(employmentInfo -> {
            metadata.put("employment.employeeNumber", toStringOrEmpty(employmentInfo.employeeNumber()));
            metadata.put("employment.jobTitle", toStringOrEmpty(employmentInfo.jobTitle()));
            metadata.put("employment.status", toStringOrEmpty(employmentInfo.status()));
            metadata.put("employment.payCategory", toStringOrEmpty(employmentInfo.payCategory()));
            metadata.put("employment.baseSalary", numberOrZero(employmentInfo.baseMonthlySalary()));
        });

        Optional.ofNullable(payslipData.period()).ifPresent(periodInfo -> {
            metadata.put("period.start", dateString(periodInfo.periodStart()));
            metadata.put("period.end", dateString(periodInfo.periodEnd()));
            metadata.put("period.payDate", dateString(periodInfo.payDate()));
            metadata.put("period.currency", toStringOrEmpty(periodInfo.currency()));
            metadata.put("period.monthKey", monthKey(periodInfo.periodStart()));
        });

        Optional.ofNullable(payslipData.financial()).ifPresent(financialInfo -> {
            metadata.put("financial.gross", numberOrZero(financialInfo.gross()));
            metadata.put("financial.taxable", numberOrZero(financialInfo.taxable()));
            metadata.put("financial.net", numberOrZero(financialInfo.net()));
            metadata.put("financial.rsz", numberOrZero(financialInfo.socialSecurity()));
            metadata.put("financial.tax", numberOrZero(financialInfo.withholdingTax()));
            metadata.put("financial.iban.last4", last4(toStringOrEmpty(financialInfo.paymentIban())));
            metadata.put("financial.bic.last4", last4(toStringOrEmpty(financialInfo.paymentBic())));
        });

        return metadata;
    }

    private static void addIfNotBlank(List<Document> documents, String text, Map<String, Object> metadata) {
        if (text != null && !text.isBlank()) documents.add(new Document(text, metadata));
    }

    private static Map<String, Object> with(Map<String, Object> baseMetadata, Map<String, Object> extraMetadata) {
        Map<String, Object> mergedMetadata = new LinkedHashMap<>(baseMetadata);
        mergedMetadata.putAll(extraMetadata);
        return mergedMetadata;
    }

    private static String composeBaseId(Map<String, Object> metadata) {
        String primaryIdentifier = getString(metadata, "personal.nationalId")
                .or(() -> getString(metadata, "employment.employeeNumber"))
                .or(() -> getString(metadata, "source"))
                .orElse("anon");

        String monthKey = getString(metadata, "period.monthKey").orElse("unknown");
        String shortHash = getString(metadata, "file.sha256").map(hash -> hash.length() > 8 ? hash.substring(0, 8) : hash).orElse("");
        return "payslip:" + primaryIdentifier + ":" + monthKey + ":" + shortHash;
    }

    private static Optional<String> getString(Map<String, Object> metadata, String key) {
        return Optional.ofNullable(metadata.get(key)).map(String::valueOf).filter(value -> !value.isBlank());
    }

    private static String monthKey(LocalDate date) {
        return Optional.ofNullable(date)
                .map(localDate -> "%d-%02d".formatted(localDate.getYear(), localDate.getMonthValue()))
                .orElse("");
    }

    private static String dateString(LocalDate date) {
        return Optional.ofNullable(date).map(localDate -> localDate.format(DateTimeFormatter.ISO_DATE)).orElse("");
    }

    private static String toStringOrEmpty(Object value) {
        return Optional.ofNullable(value).map(String::valueOf).orElse("");
    }

    private static String moneyString(BigDecimal value) {
        return Optional.ofNullable(value)
                .map(amount -> amount.stripTrailingZeros().toPlainString())
                .orElse("0");
    }

    private static Number numberOrZero(BigDecimal value) {
        return Optional.ofNullable(value).orElse(BigDecimal.ZERO);
    }

    private static String last4(String text) {
        String normalizedText = Optional.ofNullable(text).orElse("").replaceAll("\\s", "");
        return normalizedText.length() <= 4 ? normalizedText : normalizedText.substring(normalizedText.length() - 4);
    }

    private static String shortAddress(String address) {
        String fullAddress = Optional.ofNullable(address).orElse("");
        return fullAddress.length() > PayslipIndexService.MAX_ADDRESS_DISPLAY_LENGTH ? fullAddress.substring(0, PayslipIndexService.MAX_ADDRESS_DISPLAY_LENGTH) + "…" : fullAddress;
    }

    private static String shortSha256(String text) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(Optional.ofNullable(text).orElse("").getBytes(StandardCharsets.UTF_8));
            return toHex(digest).substring(0, 8);
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 should always be available, but fallback just in case
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return toHex(messageDigest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            return "";
        }
    }

    private static String toHex(byte[] data) {
        StringBuilder stringBuilder = new StringBuilder(data.length * 2);
        for (byte byteValue : data) stringBuilder.append(String.format("%02x", byteValue));
        return stringBuilder.toString();
    }

    private record DocPart(String text, Map<String, Object> metadata) {
    }
}