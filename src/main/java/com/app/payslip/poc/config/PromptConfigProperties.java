package com.app.payslip.poc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.prompts")
public class PromptConfigProperties {

    private PayslipPrompts payslip;

    @Data
    public static class PayslipPrompts {
        private String systemExtraction;
        private String userExtraction;

        private String systemCompare;
        private String userCompare;

        private String systemAsk;
    }
}