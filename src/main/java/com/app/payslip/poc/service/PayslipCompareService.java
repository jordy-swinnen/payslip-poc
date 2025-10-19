package com.app.payslip.poc.service;

import com.app.payslip.poc.config.PromptConfigProperties;
import com.app.payslip.poc.model.ExtractedPayslipDataDTO;
import com.app.payslip.poc.model.TextAnswerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayslipCompareService {

    private final ChatClient chatClient;
    private final PayslipComparator payslipComparator;
    private final PayslipExtractionService payslipExtractionService;
    private final PromptConfigProperties promptConfig;

    public TextAnswerDTO compareAndExplain(
            MultipartFile previousPayslipFile,
            MultipartFile currentPayslipFile,
            String userQuestion
    ) throws IOException {
        ExtractedPayslipDataDTO previousPayslip = payslipExtractionService.scrapeAndIndexPayslip(previousPayslipFile);
        ExtractedPayslipDataDTO currentPayslip = payslipExtractionService.scrapeAndIndexPayslip(currentPayslipFile);

        String previousMonthKey = monthKey(previousPayslip);
        String currentMonthKey = monthKey(currentPayslip);
        String employeeNumber = Optional.ofNullable(currentPayslip.employment()).map(ExtractedPayslipDataDTO.EmploymentInfo::employeeNumber).orElse("");
        String nationalId = Optional.ofNullable(currentPayslip.personal()).map(ExtractedPayslipDataDTO.PersonalInfo::nationalId).orElse("");

        String userMessage = promptConfig.getPayslip().getUserCompare().formatted(
                userQuestion,
                previousMonthKey,
                currentMonthKey,
                employeeNumber,
                nationalId,
                JsonView.from(previousPayslip),
                JsonView.from(currentPayslip)
        );

        String answer = chatClient
                .prompt()
                .system(promptConfig.getPayslip().getSystemCompare())
                .tools(payslipComparator)
                .user(userMessage)
                .call()
                .content();

        return TextAnswerDTO.builder().answer(answer).build();
    }

    private String monthKey(ExtractedPayslipDataDTO dto) {
        return Optional.ofNullable(dto)
                .map(ExtractedPayslipDataDTO::period)
                .map(ExtractedPayslipDataDTO.PeriodInfo::periodStart)
                .map(d -> "%d-%02d".formatted(d.getYear(), d.getMonthValue()))
                .orElse("");
    }

    static final class JsonView {
        static String from(ExtractedPayslipDataDTO dto) {
            if (dto == null) return "{}";
            String personal = dto.personal() == null ? "{}" : """
                    {
                      "name":"%s",
                      "nationalId":"%s",
                      "maritalStatus":"%s",
                      "dependents":%s
                    }""".formatted(
                    safe(dto.personal().name()),
                    safe(dto.personal().nationalId()),
                    safe(dto.personal().maritalStatus()),
                    Optional.ofNullable(dto.personal().dependents()).map(String::valueOf).orElse("0")
            ).trim();

            String employer = dto.employer() == null ? "{}" : """
                    {
                      "name":"%s",
                      "employerNumber":"%s"
                    }""".formatted(
                    safe(dto.employer().name()),
                    safe(dto.employer().employerNumber())
            ).trim();

            String employment = dto.employment() == null ? "{}" : """
                    {
                      "employeeNumber":"%s",
                      "jobTitle":"%s",
                      "status":"%s",
                      "payCategory":"%s",
                      "baseMonthlySalary":"%s"
                    }""".formatted(
                    safe(dto.employment().employeeNumber()),
                    safe(dto.employment().jobTitle()),
                    safe(dto.employment().status()),
                    safe(dto.employment().payCategory()),
                    dto.employment().baseMonthlySalary() == null ? "0" : dto.employment().baseMonthlySalary().toPlainString()
            ).trim();

            String period = dto.period() == null ? "{}" : """
                    {
                      "periodStart":"%s",
                      "periodEnd":"%s",
                      "payDate":"%s",
                      "currency":"%s"
                    }""".formatted(
                    dto.period().periodStart() == null ? "" : dto.period().periodStart().toString(),
                    dto.period().periodEnd() == null ? "" : dto.period().periodEnd().toString(),
                    dto.period().payDate() == null ? "" : dto.period().payDate().toString(),
                    safe(dto.period().currency())
            ).trim();

            String financial = dto.financial() == null ? "{}" : """
                    {
                      "gross":"%s",
                      "taxable":"%s",
                      "socialSecurity":"%s",
                      "withholdingTax":"%s",
                      "net":"%s"
                    }""".formatted(
                    dto.financial().gross() == null ? "0" : dto.financial().gross().toPlainString(),
                    dto.financial().taxable() == null ? "0" : dto.financial().taxable().toPlainString(),
                    dto.financial().socialSecurity() == null ? "0" : dto.financial().socialSecurity().toPlainString(),
                    dto.financial().withholdingTax() == null ? "0" : dto.financial().withholdingTax().toPlainString(),
                    dto.financial().net() == null ? "0" : dto.financial().net().toPlainString()
            ).trim();

            String extras = dto.extras() == null ? "{}" : """
                    {
                      "mealVoucherContributionEmployer":"%s",
                      "mealVoucherContributionEmployee":"%s",
                      "mealVoucherCount":%s
                    }""".formatted(
                    dto.extras().mealVoucherContributionEmployer() == null ? "0" : dto.extras().mealVoucherContributionEmployer().toPlainString(),
                    dto.extras().mealVoucherContributionEmployee() == null ? "0" : dto.extras().mealVoucherContributionEmployee().toPlainString(),
                    Optional.ofNullable(dto.extras().mealVoucherCount()).map(String::valueOf).orElse("0")
            ).trim();

            return """
                    {
                      "personal": %s,
                      "employer": %s,
                      "employment": %s,
                      "period": %s,
                      "financial": %s,
                      "extras": %s
                    }""".formatted(personal, employer, employment, period, financial, extras).trim();
        }

        static String safe(String value) {
            return Optional.ofNullable(value).orElse("").replace("\"", "\\\"");
        }
    }
}
